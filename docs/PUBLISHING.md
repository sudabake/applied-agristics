# 一键发布

## 首次配置

仓库的 Actions 页面提供 Publish release。项目固定参数在 .github/release.json，CurseForge 为 1772431，Modrinth 为 applied-agristics。两平台均声明 AE2 和 Mystical Agriculture 为必需依赖，GuideME 与 Cucumber 由前置继续带入。

在 GitHub 仓库的 Settings → Secrets and variables → Actions → New repository secret 添加两个密钥。

| 名称 | 内容 |
|---|---|
| CURSEFORGE_TOKEN | CurseForge 作者上传 API token |
| MODRINTH_TOKEN | 拥有该项目读取与创建版本权限的 Modrinth token |

GitHub 发布使用运行时自动提供的 GITHUB_TOKEN。令牌只填在 Secrets 页面。上传前会检查所选平台的令牌，Modrinth 项目通过令牌解析为真实项目 ID。若项目还没有公开，检查取决于令牌能否访问它。

令牌入口为 https://authors.curseforge.com/account/api-tokens 和 https://modrinth.com/settings/pats 。GitHub 密钥页面为 https://github.com/sudabake/applied-agristics/settings/secrets/actions 。

## 每次发布

1. 将已验证的公开源码推送到准备发布的分支，更新 gradle.properties 的 mod_version 和 docs/RELEASE_NOTES.md。
2. 打开 Actions → Publish release → Run workflow，选择源码分支。
3. 填版本号和发行类型，选择 all 或单个平台。
4. 首次保留“仅测试并生成下载包”的勾选，成功后可从运行的 Artifacts 下载检查。
5. 正式上传时取消该勾选，再运行一次。

稳定版使用 1.3.1 与 release。测试版使用 1.3.2-beta.1 与 beta，或 1.3.2-alpha.1 与 alpha。版本号核心必须与所选源码的 mod_version 一致，例如源码 1.3.1-dev 可以构建 1.3.1。更新日志中英文写在同一个文件，三个平台共用。

只配置工作流时，公开分支仍可能保留上一版源码。要发布本地新功能，先完成源码同步。工作流会拒绝给旧源码套上下一版核心版本号。

## 自动完成的工作

- 运行资源工具测试、资源重建、JUnit 及默认／最低核心依赖的游戏测试，全部成功后才开放上传。
- 构建内部版本与文件名一致的 JAR，打包当前公开提交的源码 ZIP 和 SHA256SUMS。
- 上传前核对提交、版本及文件摘要。三个平台使用同一份 JAR。
- CurseForge 和 Modrinth 各创建独立的新版本，仅上传运行 JAR。GitHub Release 同时带源码 ZIP 和校验文件。
- 自动填 Minecraft 1.21.1、NeoForge、Java 21、发行类型、更新日志及固定前置关系。

普通 push 和 pull_request 继续只运行 build.yml 的构建检查。Publish release 仅接受手动运行。演练不会创建版本或上传发行附件，生成文件保存在该运行的 Artifacts，保留 14 天。

## 部分平台失败

三个平台分开运行，结果可分别查看。先检查失败平台是否实际收到文件，再使用 Re-run failed jobs 重跑失败作业，成功的平台继续保留结果。完整重新发起同一版本会被已有 GitHub 标签检查拒绝。

上传工具为 mc-publish 3.3.2，固定到已核对的提交。CurseForge 文件上传成功后仍遵循平台审核流程。自动化仅验证上传前准备与接口返回，实际下载页仍需按运行结果核对。
