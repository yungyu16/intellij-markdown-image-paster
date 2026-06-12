# 发布指南

## 概述

推送符合 `v*` 格式的 Git Tag，GitHub Actions 自动完成：

1. **验证** — `verifyPlugin` 检查插件结构
2. **构建** — 生成插件 ZIP
3. **签名** — 使用 JetBrains 签名证书对 ZIP 签名
4. **发布** — 上传到 JetBrains Marketplace（进入审核队列，约 2 个工作日）
5. **GitHub Release** — 创建 Release 并附 ZIP

---

## 一、前置准备（仅首次）

### 1. 获取 Marketplace Publish Token

1. 打开 [JetBrains Marketplace → My Tokens](https://plugins.jetbrains.com/author/me/tokens)
2. 填写 Token 名称，点击 **"Generate Token"**
3. **立即复制**（关闭页面后不可再查看）

### 2. 生成插件签名密钥

JetBrains 要求所有上架插件必须签名，需本地用 `openssl` 生成密钥对：

```bash
# 步骤 1：生成 4096 位 RSA 私钥（会提示设置密码，记住它）
openssl genpkey \
  -aes-256-cbc \
  -algorithm RSA \
  -out private_encrypted.pem \
  -pkeyopt rsa_keygen_bits:4096

# 步骤 2：导出无密码的私钥（用于 GitHub Secret）
openssl rsa \
  -in private_encrypted.pem \
  -out private.pem

# 步骤 3：生成自签名证书链（有效期 365 天）
openssl req \
  -key private.pem \
  -new \
  -x509 \
  -days 365 \
  -out chain.crt
```

执行后得到：
- `private.pem` — 私钥
- `chain.crt` — 证书链

> `private_encrypted.pem` 可删除，`private.pem` 和 `chain.crt` 不要提交到代码仓库。

### 3. 配置 GitHub Secrets

打开仓库 **Settings → Secrets and variables → Actions → New repository secret**，添加以下四个：

| Secret 名称 | 内容 |
|-------------|------|
| `JETBRAINS_TOKEN` | 步骤 1 生成的 Publish Token |
| `CERTIFICATE_CHAIN` | `chain.crt` 完整内容（含 `-----BEGIN CERTIFICATE-----` 行） |
| `PRIVATE_KEY` | `private.pem` 完整内容（含 `-----BEGIN RSA PRIVATE KEY-----` 行） |
| `PRIVATE_KEY_PASSWORD` | 步骤 2 设置的私钥密码（若导出时未加密则留空） |

---

## 二、发布新版本

```bash
# 1. 打 tag（版本号即 tag 去掉 v 前缀，CI 自动注入）
git tag v1.0.1

# 2. 推送 tag 触发 CI
git push origin v1.0.1
```

CI 从 tag 名提取版本号（`v1.0.1` → `1.0.1`），通过 `PLUGIN_VERSION` 环境变量注入构建，无需手动修改 `build.gradle.kts`。

### 版本号规范

| Tag | 含义 |
|-----|------|
| `v1.0.1` | 补丁修复 |
| `v1.1.0` | 新功能（向后兼容） |
| `v2.0.0` | 不兼容变更 |

---

## 三、故障排查

| 问题 | 可能原因 | 解决方式 |
|------|----------|----------|
| 签名失败 | 密钥内容不完整或格式有误 | 重新复制 `chain.crt` / `private.pem` 全文（包含首尾 `-----` 行） |
| 发布 401/403 | Token 过期或无效 | 在 Marketplace → My Tokens 重新生成 |
| 发布 400 | 该版本号已存在 | 递增版本号，重新打 tag |
| `signPlugin` 被跳过 | Secret 名称拼写错误 | 确认四个 Secret 名称与工作流 `env` 块完全一致 |
| GitHub Release 未创建 | 由 `workflow_dispatch` 手动触发 | 手动触发不创建 Release，需推送 tag |
| Marketplace 版本号未更新 | 发布后需等待 JetBrains 审核（约 2 个工作日） | 在 [Marketplace 管理页](https://plugins.jetbrains.com/author/me) 查看审核状态 |

## 发布后验证

```bash
# 确认 GitHub Release 已创建
gh release view v1.0.1

# 确认插件 ZIP 已上传至 Release 资产
gh release download v1.0.1 --pattern "*.zip"
```

---

## 参考

- [IntelliJ Platform Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
