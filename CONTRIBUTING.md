# 贡献指南

欢迎贡献！以下是参与开发的指引。

## 项目概述

**intellij-claude-code-terminal** 是一个 JetBrains IDE 插件，提供一键聚焦或创建 Claude Code Terminal tab 的功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 2.1.20 | 主语言 |
| IntelliJ Platform Gradle Plugin | 2.5.0 | 构建插件 |
| Target IDE | IDEA Community 2024.2 (build 242) | 最低兼容版本 |
| JDK | 17 | 构建和运行时要求 |

## 环境准备

- JDK 17（建议通过 [jenv](https://www.jenv.be/) 管理）
- JetBrains IDE（开发调试用）

项目根目录已包含 `.java-version`，jenv 会自动切换。

## 构建与运行

```bash
make build    # 构建插件 ZIP（产出在 build/distributions/）
make run      # 在沙箱 IDEA 中运行（首次需下载 IDE，耗时较长）
make clean    # 清理构建产物
make dist     # 查看构建产物路径和大小
make publish  # 构建并发布插件到 JetBrains Marketplace
make help     # 查看可用命令
```

Makefile 通过 `$(shell jenv javahome)` 动态获取 JAVA_HOME。

## 项目结构

```
src/main/kotlin/io/github/yungyu16/claudecodeterminal/
├── ClaudeTerminalAction.kt                  # 核心 Action：聚焦或创建 Terminal tab
├── ClaudeTerminalSettings.kt                # 应用级用户设置 + 命令构建
├── ClaudeTerminalSettingsConfigurable.kt    # 设置界面

src/main/resources/META-INF/plugin.xml       # 插件结构声明
build.gradle.kts                             # 构建配置 + 插件元数据
Makefile                                     # 构建快捷命令
```

## 关键设计

> 详情参见 [AGENTS.md](AGENTS.md)。

### 防并发

`creationInProgress` 和 `healthCheckInProgress` 使用 `WeakHashMap<Project/Content, Boolean>` 实现，对象 GC 后自动清理，杜绝内存泄漏。当前调用链均为 EDT 同步，保留此结构防御外部绕过 EDT 的调用。

### Tab 标识

通过 `Content.putUserData(Key)` 打标记（`CLAUDE_TAB_KEY` / `LOCAL_TAB_KEY`）来识别 tab，而非依赖 `displayName`，避免终端插件本地化导致 displayName 不稳定。

### 进程存活检测

- **3 秒冷却期**（`CREATION_COOLDOWN_MS`）：覆盖 Claude 启动到 shell integration 注册前台进程之间的窗口，避免误判
- **保守策略**：无法获取 widget 或检测异常时返回 true（存活），避免误删有效 tab
- **检测方式**：通过 `ShellTerminalWidget.hasRunningCommands()` 判断前台进程是否存在；`TtyConnector.isConnected` 判断 shell 是否存活
- **后台线程**：`hasRunningCommands()` 要求非 EDT 线程，检测结果回 EDT 执行 UI 操作

## 设计决策

> 并发安全设计、命名规范、编码注意事项等关键决策参见 [AGENTS.md](AGENTS.md)。

## 发布

发布由专门的维护者操作，流程详见 [RELEASE.md](RELEASE.md)。

## 如何贡献

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feat/my-feature`
3. 提交修改：`git commit -m "feat: add my feature"`
4. 推送到分支：`git push origin feat/my-feature`
5. 提交 Pull Request

### Commit 规范

建议使用 [Conventional Commits](https://www.conventionalcommits.org/) 风格：

- `feat: ...` — 新功能
- `fix: ...` — 修复
- `docs: ...` — 文档
- `refactor: ...` — 重构
- `chore: ...` — 构建/工具链

## License

[MIT](LICENSE)
