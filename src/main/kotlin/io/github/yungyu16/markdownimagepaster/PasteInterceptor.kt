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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import io.github.yungyu16.markdownimagepaster.image.ImageFormatDetector
import io.github.yungyu16.markdownimagepaster.image.ImageWriter
import java.awt.datatransfer.Transferable
import java.io.IOException
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
        val handlerToRegister: PasteImageActionHandler? = synchronized(lock) {
            if (!activeProjects.add(project)) {
                return
            }
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
        } catch (e: Exception) {
            Logger.getInstance(PasteImageActionHandler::class.java).debug("CopyPasteManager contents unavailable", e)
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
        val parent = file.parent
        val fileParentPath: String = if (parent == null) {
            ""
        } else {
            try {
                Path.of(projectBasePath).relativize(Path.of(parent.path)).toString().replace('\\', '/') + "/"
            } catch (_: IllegalArgumentException) {
                original.execute(editor, caret, dataContext)
                return
            }
        }

        val document = editor.document
        val targetDir = PasteSupport.resolveTargetDir(document.text, fileBaseName, fileParentPath)
        if (targetDir == null) {
            original.execute(editor, caret, dataContext)
            return
        }

        val pool = ApplicationManager.getApplication()
        pool.executeOnPooledThread {
            val absoluteTargetDir = Path.of(projectBasePath, targetDir)
            val baseName = PasteSupport.imageFileName(imageData.sourceName)

            val writeTarget = try {
                ImageWriter.prepareWrite(
                    imageData.image,
                    imageData.format,
                    absoluteTargetDir,
                    Path.of(projectBasePath)
                )
            } catch (e: Exception) {
                Logger.getInstance(PasteImageActionHandler::class.java).warn("Failed to prepare pasted image", e)
                return@executeOnPooledThread
            }

            pool.invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    val projectRoot = LocalFileSystem.getInstance().findFileByPath(projectBasePath)
                    if (projectRoot == null) {
                        Logger.getInstance(PasteImageActionHandler::class.java).warn("Project root not found in VFS")
                        return@runWriteCommandAction
                    }

                    val dirVFile = try {
                        VfsUtil.createDirectoryIfMissing(projectRoot, targetDir)
                    } catch (e: IOException) {
                        Logger.getInstance(PasteImageActionHandler::class.java).warn("Failed to create directory for pasted image", e)
                        return@runWriteCommandAction
                    } ?: run {
                        Logger.getInstance(PasteImageActionHandler::class.java).warn("Failed to create directory for pasted image")
                        return@runWriteCommandAction
                    }

                    val ext = imageData.format
                    val resolvedName = resolveName(dirVFile, baseName, ext)
                    val imageFileName = "$resolvedName.$ext"
                    val child = try {
                        dirVFile.createChildData(project, imageFileName)
                    } catch (e: IOException) {
                        Logger.getInstance(PasteImageActionHandler::class.java).warn("Failed to create VFS file for pasted image", e)
                        return@runWriteCommandAction
                    }
                    child.setBinaryContent(writeTarget.bytes)

                    val projectRelativePath = targetDir + imageFileName
                    val markdownPath = PasteSupport.markdownImagePath(projectRelativePath)
                    val displayName = imageFileName.substringBeforeLast('.')
                    val markdown = "![$displayName]($markdownPath)"

                    val currentOffset = editor.caretModel.offset
                    val safeOffset = currentOffset.coerceIn(0, document.textLength)
                    if (safeOffset != currentOffset) return@runWriteCommandAction
                    try {
                        document.insertString(safeOffset, markdown)
                    } catch (e: Exception) {
                        Logger.getInstance(PasteImageActionHandler::class.java).warn("Failed to insert markdown for pasted image", e)
                        return@runWriteCommandAction
                    }
                    if (editor.caretModel.offset == safeOffset) {
                        editor.caretModel.moveToOffset((safeOffset + markdown.length).coerceAtMost(document.textLength))
                    }
                }
            }
        }
    }

    private fun isMarkdown(file: VirtualFile): Boolean =
        file.extension?.lowercase() in MARKDOWN_EXTENSIONS

    private fun resolveName(dir: VirtualFile, baseName: String, ext: String): String {
        if (dir.findChild("$baseName.$ext") == null) return baseName
        var counter = 2
        while (counter <= MAX_COLLISION_COUNTER) {
            if (dir.findChild("$baseName-$counter.$ext") == null) {
                return "$baseName-$counter"
            }
            counter++
        }
        throw IOException("Could not find a non-conflicting filename for '$baseName' after $MAX_COLLISION_COUNTER attempts")
    }

    companion object {
        private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd")
        private const val MAX_COLLISION_COUNTER = 10_000
    }
}
