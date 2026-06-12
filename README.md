# Markdown Image Paster

[![License](https://img.shields.io/github/license/yungyu16/intellij-markdown-image-paster)](LICENSE)
[![Platform](https://img.shields.io/badge/IDE-2024.2%2B-blue)](https://plugins.jetbrains.com/plugin/32253-markdown-image-paster)
[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/32253-markdown-image-paster?label=Marketplace)](https://plugins.jetbrains.com/plugin/32253-markdown-image-paster)

在 Markdown 文件中粘贴图片时，根据 front matter 规则自动保存到正确目录。

- GitHub: <https://github.com/yungyu16/intellij-markdown-image-paster>
- JetBrains Marketplace: <https://plugins.jetbrains.com/plugin/32253-markdown-image-paster>

## 背景

Jekyll / Hexo 等静态博客的文章文件和图片目录有严格的路径对应关系。例如：

```
_posts/2026-06-11-我的文章.md          ← 文章
img/2026-06-11-我的文章/cover.png      ← 图片目录（目录名 = 文件名去后缀）
```

IntelliJ 内置 Markdown 插件不支持根据文件名动态决定图片路径，现有社区插件也不支持读取 front matter 做路径路由。本插件弥补了这一能力。

## 功能

- **粘贴拦截** — 仅在带 `typora-root-url` 或 `media_subpath` 的 Markdown 文件中自动触发路由逻辑，其它情况走 IDE 默认粘贴
- **Front matter 感知** — 按优先级解析路径：
  1. `typora-root-url`（Typora 兼容）
  2. `media_subpath`（Jekyll Chirpy 主题约定）
- **目录复用** — 同一篇文章始终保存到同一套规则解析出的图片目录
- **稳定输出** — 剪贴板图片统一写为 PNG，避免不同系统剪贴板格式差异
- **自动命名** — 优先使用剪贴板图片文件名，拿不到时使用 `image`，并追加 `yyyyMMddHHmmss` 时间戳
- **稳定引用** — 插入的 Markdown 图片路径以 project 根目录 `/` 开头，并按 URL path segment 编码

## 路径决策

| 优先级 | Front Matter 字段 | 含义 | 示例结果 |
|--------|-------------------|------|----------|
| 1 | `typora-root-url` | Typora 资源根路径，解析后追加 `img/<basename>/` | `/assets` → `assets/img/<basename>/` |
| 2 | `media_subpath` | 目标图片目录本身 | `/img/2026-06-11-xxx/` → `img/2026-06-11-xxx/` |

`typora-root-url` 和 `media_subpath` 中的路径值遵循同一套解析规则：

- 以 `/` 开头：从当前 project 根目录开始解析
- 不以 `/` 开头：从当前 Markdown 文件所在目录开始解析
- 规范化后不能逃逸出当前 project；如果路径越界，插件不接管本次粘贴，回到 IDE 默认行为
- 如果 front matter 中没有 `typora-root-url` 或 `media_subpath`，插件不接管本次粘贴，回到 IDE 默认行为

## 安装

### 方式一：Marketplace

在 IDE **Settings → Plugins → Marketplace** 搜索 "Markdown Image Paster" 安装，或直接打开 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32253-markdown-image-paster)。

### 方式二：手动安装

1. 从 [GitHub Releases](https://github.com/yungyu16/intellij-markdown-image-paster/releases) 下载最新 `.zip`
2. IDE 内 **Settings → Plugins → ⚙ → Install Plugin from Disk...**，选择 `.zip`，重启

## 使用

1. 打开带 front matter 的 `.md` 文件
2. 复制一张图片（截图或文件）
3. 在编辑器中粘贴（Ctrl+V / Cmd+V）
4. 图片自动保存到正确目录，并插入 `![name](path)` 代码

文件名格式为 `<图片原文件名>-yyyyMMddHHmmss.png`。如果剪贴板没有提供图片文件名，则使用 `image-yyyyMMddHHmmss.png`。

插入的 Markdown 图片引用使用从 project 根目录开始的 `/` 路径，并只对路径片段做 URL encode：

```markdown
![image-20260612211415](/img/%E6%96%87%E7%AB%A0/image-20260612211415.png)
```

## 环境要求

- JetBrains IDE **2024.2+**（IntelliJ IDEA、GoLand、PyCharm 等）

## License

[MIT](LICENSE)
