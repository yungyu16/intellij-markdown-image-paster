# AGENTS.md — AI 协作指南

本文件为 AI 编程助手提供项目上下文。

## 项目概述

**Markdown Image Paster** 是一个 JetBrains IDE 插件。在 Markdown 文件中粘贴图片时，根据 front matter 规则自动将图片保存到正确目录。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 2.1.20 | 主语言 |
| IntelliJ Platform Gradle Plugin | 2.5.0 | 构建插件 |
| Target IDE | IDEA Community 2024.2 (build 242) | 最低兼容版本 |
| JDK | 17 | 构建和运行时要求 |

## 分层架构

```
paste (入口)         PasteInterceptor        拦截粘贴，判断条件，委托给下层
  ├─ image (图片层)   ImageFormatDetector     识别剪贴板图片格式
  │                   ImageWriter             写入文件系统，处理文件名冲突和路径越界校验
  ├─ path  (路径层)   FrontMatterParser       解析 YAML front matter
  │                   PathResolver           按优先级决策目标路径
  └─ PasteSupport     纯逻辑编排：front matter 判断、目标目录、文件名
```

依赖方向：paste → image / path。image、path、PasteSupport 是纯逻辑或弱 IntelliJ 依赖，方便单测。

## 关键文件

```
src/main/kotlin/io/github/yungyu16/markdownimagepaster/
├── PasteSupport.kt              # front matter 判断 + 路径/文件名决策（无 IntelliJ API）
├── path/
│   ├── FrontMatter.kt           # YAML front matter 解析 + 数据结构
│   └── PathResolver.kt          # 路径决策（typora-root-url → media_subpath → 默认）
├── image/
│   ├── ImageFormat.kt           # 从 Transferable 提取 BufferedImage + 格式检测
│   └── ImageWriter.kt           # ImageIO.write 封装 + 文件名冲突和路径越界校验
└── PasteInterceptor.kt          # 入口：ProjectManagerListener，拦截粘贴并编排流程

src/main/resources/META-INF/plugin.xml   # 插件描述符
build.gradle.kts                         # 构建配置
```

## 路径决策规则

1. **typora-root-url** — Typora 兼容语义：字段值表示资源根路径，解析后拼接 `img/<basename>/`
2. **media_subpath** — 字段值表示目标图片目录本身
3. **无认识字段** — 不接管本次粘贴，委托 IDE 原始行为

`typora-root-url` 和 `media_subpath` 的字段值统一遵循：

- 以 `/` 开头：从当前 project 根目录解析
- 不以 `/` 开头：从当前 Markdown 文件所在目录解析
- 规范化后不能逃逸出当前 project；越界时不接管本次粘贴，委托 IDE 原始行为
- front matter 中没有 `typora-root-url` 或 `media_subpath` 时不接管本次粘贴

## 构建与运行

```bash
./gradlew buildPlugin   # 构建插件 ZIP
./gradlew runIde        # 在沙箱 IDEA 中运行
./gradlew clean         # 清理产物
```

## 注意事项

- `untilBuild` 设为 `null`（无上限）
- 没有 front matter，或 front matter 中没有 `typora-root-url` / `media_subpath` 时，不接管图片粘贴，完全委托 IDE 原始行为
- 图片文件名不弹窗，优先使用剪贴板图片原文件名，拿不到时 fallback 到 `image`，并追加 `yyyyMMddHHmmss`
- Markdown 图片引用使用从 project 根开始的 `/` 路径，并对 path segment 做 URL encode；磁盘真实路径不 encode
- `PasteInterceptor` 通过 `EditorActionManager.setEditorActionHandler()` 代理 `IdeActions.ACTION_EDITOR_PASTE`，拦截不到时委托给 `originalHandler`
- 非 Markdown 文件、剪贴板无图片时完全委托给原始行为，不做任何额外操作
