package io.github.yungyu16.markdownimagepaster

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import io.github.yungyu16.markdownimagepaster.image.ImageFormatDetector
import io.github.yungyu16.markdownimagepaster.image.ImageWriter
import java.awt.datatransfer.Transferable
import java.nio.file.Path

class PasteHandlerRegistrar : StartupActivity {

    override fun runActivity(project: Project) {
        val app = ApplicationManager.getApplication()
        if (app.isDisposed) return
        app.invokeLater {
            val mgr = EditorActionManager.getInstance()
            register(project, mgr)
        }
    }

    private fun register(project: Project, mgr: EditorActionManager) {
        val handlerToRegister = synchronized(lock) {
            activeProjects.add(project)
            val current = mgr.getActionHandler(IdeActions.ACTION_EDITOR_PASTE)
            if (current is PasteImageActionHandler) null else {
                PasteImageActionHandler(current).also { installedHandler = it }
            }
        }

        handlerToRegister?.let { mgr.setActionHandler(IdeActions.ACTION_EDITOR_PASTE, it) }

        Disposer.register(project, Disposable {
            unregister(project, mgr)
        })
    }

    private fun unregister(project: Project, mgr: EditorActionManager) {
        val handlerToRestore = synchronized(lock) {
            activeProjects.remove(project)
            val current = installedHandler
            if (activeProjects.isEmpty() && current != null) {
                installedHandler = null
                current
            } else {
                null
            }
        }

        handlerToRestore?.let { handler ->
            if (mgr.getActionHandler(IdeActions.ACTION_EDITOR_PASTE) === handler) {
                mgr.setActionHandler(IdeActions.ACTION_EDITOR_PASTE, handler.original)
            }
        }
    }

    companion object {
        private val lock = Any()
        private val activeProjects = mutableSetOf<Project>()
        private var installedHandler: PasteImageActionHandler? = null
    }
}

private class PasteImageActionHandler(
    val original: EditorActionHandler
) : EditorActionHandler() {

    override fun doExecute(
        editor: Editor,
        caret: Caret?,
        dataContext: com.intellij.openapi.actionSystem.DataContext
    ) {
        val project = dataContext.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT)
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (project == null || file == null || !isMarkdown(file)) {
            original.execute(editor, caret, dataContext)
            return
        }

        val contents: Transferable? = try {
            CopyPasteManager.getInstance().contents
        } catch (_: Exception) {
            null
        }
        val imageData = ImageFormatDetector.detect(contents)
        if (imageData == null) {
            original.execute(editor, caret, dataContext)
            return
        }

        val projectBasePath = project.basePath
        if (projectBasePath == null) {
            original.execute(editor, caret, dataContext)
            return
        }

        val fileBaseName = file.nameWithoutExtension
        val fileParentPath = file.parent?.let { parent ->
            val projectRoot = Path.of(projectBasePath)
            val parentPath = Path.of(parent.path)
            projectRoot.relativize(parentPath).toString().replace('\\', '/') + "/"
        } ?: ""

        val startOffset = editor.caretModel.offset
        val document = editor.document
        val documentText = document.text
        val targetDir = PasteSupport.resolveTargetDir(documentText, fileBaseName, fileParentPath)
        if (targetDir == null) {
            original.execute(editor, caret, dataContext)
            return
        }

        val pool = ApplicationManager.getApplication()
        pool.executeOnPooledThread {
            val absoluteTargetDir = Path.of(projectBasePath, targetDir)
            val fileName = PasteSupport.imageFileName(imageData.sourceName)

            val projectRelativePath = try {
                ImageWriter.write(
                    imageData.image,
                    imageData.format,
                    absoluteTargetDir,
                    fileName,
                    Path.of(projectBasePath)
                )
            } catch (e: Exception) {
                Logger.getInstance(PasteImageActionHandler::class.java).warn("Failed to write pasted image", e)
                return@executeOnPooledThread
            }

            val markdownPath = PasteSupport.markdownImagePath(projectRelativePath)
            val markdown = "![$fileName]($markdownPath)"
            pool.invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    val safeOffset = startOffset.coerceIn(0, document.textLength)
                    document.insertString(safeOffset, markdown)
                    if (editor.caretModel.offset == safeOffset) {
                        editor.caretModel.moveToOffset((safeOffset + markdown.length).coerceAtMost(document.textLength))
                    }
                }
            }
        }
    }

    private fun isMarkdown(file: VirtualFile): Boolean =
        file.extension?.lowercase() in MARKDOWN_EXTENSIONS

    companion object {
        private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd")
    }
}
