# 本机启动与维护

本项目已在这台 Windows 电脑上配置好完整运行环境。以下命令在项目目录 `collab-office` 的 PowerShell 7 中执行。

## 打开与关闭

客户交付优先使用上一级目录的 `客户一键部署.cmd`。它以 Docker Desktop 为唯一前置条件，自动检查 Compose、部署文件和离线镜像；通过后启动容器并打开 `http://localhost:3030`。客户部署细节见上一级目录的 `客户部署说明.md`。

推荐双击上一级项目目录的 `启动项目.cmd`。窗口会先检测本机环境，通过后显示“允许启动”；点击“一键启动并打开网页”即可。缺失项可查看下载和处理方法，源码与启动日志也可直接查看。详细说明见 `一键启动说明.md`。以下命令入口继续保留。

```powershell
./start-local.ps1
```

启动器会在独立后台进程中依次启动数据库、缓存、注册中心、消息队列、文件存储、搜索、各后端服务和前端。看到下面日志表示启动完成，通常需要几分钟：

```text
All local components are listening. Web: http://127.0.0.1:3000/
```

查看进度：

```powershell
Get-Content ./tools/native-runtime/startup.log -Tail 25 -Wait
```

浏览器地址：<http://127.0.0.1:3000/>。演示账号保存在 `tools/native-runtime/demo-account.json`，也可在登录页注册本地账号，无需短信或第三方账号。

当前演示用户名为 `admin`，密码以本机账号文件为准。登录后在左上角选择“产品协作演示”查看工作台数据；“PRD 功能验收”包含完整功能验收的文档、表格、导图、白板、群聊、模板和知识；“空状态验收”用于验证无任务/文档页面。会议是当前账号的日程，不按团队切换。不同浏览器分别保存登录状态和所选团队，刷新会保留当前团队；云文档只显示文字文档，工作台包含多种文档类型。演示步骤见 `Demo演示说明.md`。

```powershell
./stop-local.ps1
```

停止服务保留全部项目数据。关闭最初启动的终端不会停止后台服务。电脑重启后，需要重新运行启动命令。

## 在 IntelliJ IDEA 中启动

IDEA 社区版没有 Spring Boot 面板和 npm 面板，本项目改用普通的 Application / Shell 运行配置，效果相同。

1. 打开 `C:\Users\AR-26710\Desktop\1-demo\1-demo`（含 `.idea` 的仓库根目录），不要只打开 `collab-office`。
2. 等待右下角 Maven 导入结束；`File > Project Structure > Project SDK` 应显示 `ms-17`（Microsoft OpenJDK 17）。
3. 在右上角运行配置下拉框选择 `99 全部后端服务`，点击绿色运行按钮。该组合配置会先执行 `0 中间件（先运行一次）`，等待 MySQL、Redis、Nacos、RabbitMQ、MinIO、Elasticsearch、Mailpit 准备完成，再启动 11 个后端服务和前端。
4. 首次启动需 1~3 分钟。全部端口监听后，网关 `http://127.0.0.1:8080/api/health` 返回 `{"status":"UP"}`，浏览器可打开 <http://127.0.0.1:3000/>。
5. 以后仍直接运行 `99 全部后端服务`；无需单独运行 `0 中间件` 或 `12 web (npm run dev)`。

各服务的端口、主类和 `.env` 变量已写入 `.idea/runConfigurations/*.xml`。这些文件与本机路径绑定，且 `.idea/` 已被 `.gitignore` 忽略，属于本机配置，不会进入仓库。

注意：单独运行某个后端服务前必须先运行中间件；只启动网关不会带起业务服务。若某个服务启动失败，先看 IDEA 控制台里的 `Could not resolve placeholder` 或连接拒绝信息。

## 数据与密码

- `.env` 存放本机服务的配置和密钥，不应提交或发送给他人。
- 数据库、上传文件、搜索索引、账号文件、日志位于 Git 忽略的 `tools/native-runtime/`。不要删除这个目录排查故障。
- 项目 MySQL 使用 `13306` 端口和独立目录，不修改原有 `3306` 端口的 Windows MySQL。
- 原始数据库按 V1 至 V12 初始化。当前启动器支持从本机版本12至22升级到23，先执行本机数据库守卫和完整SQL备份，再逐版本执行尚未应用的迁移；成功后记录版本，重复启动不会重复迁移。当前实例版本为23，脚本见 `tools/update-local-schema.ps1`。V23增加平台身份和工作汇报，原项目的团队成员回填为项目成员。
- 备份位于 `tools/native-runtime/before-v*.sql`。其他部署环境应自行审核迁移；本机数据库没有建立 Flyway 历史，不能直接启用 Flyway。

## 修改后的构建

Windows 会锁定正在运行的 jar，因此先停止，再构建：

```powershell
./stop-local.ps1
mvn --batch-mode package
Push-Location ./collab-office-web
npm run build
Pop-Location
./start-local.ps1
```

`mvn package` 包含后端测试，`npm run build` 包含 TypeScript 检查。仓库目前没有独立前端单元测试或 lint 命令。`start-local.ps1 -Build` 是快速重建入口，会跳过后端测试，不能替代上述验收。

全仓测试若还要重新打开本机已下载的Word/PDF验收产物，执行下面命令；缺此参数时`ExportFileTest.downloadedApiArtifactsCanBeReopened`会按既有条件跳过。仅适用于本机保留了这两个产物的环境。

```powershell
mvn --batch-mode test "-Dprd.export.directory=C:/Users/AR-26710/Desktop/1-demo/1-demo/collab-office/tools/native-runtime"
```

## 功能验收

服务启动完成后运行：

```powershell
./tools/check-collaboration.ps1
./tools/check-local-office.ps1
```

第一个脚本创建独立验收团队和测试账号，验证邀请成员、文档、收藏、评论、权限、文件、任务、会议、两个账号实时聊天、表格、导出、分享和回收站。数据会保留供排查，重复执行会产生新的验收团队。

当前脚本在首次业务写入前复用seed数据库守卫，并保存 `before-demo-acceptance-*.sql` 完整备份；也验证纪要事项重复转任务仅生成一个任务、取消会议写入合法状态。最终验收记录见 `PRD补齐验收.md`。

仅需更新演示数据或检查幂等时：

```powershell
./tools/seed-local-demo.ps1 -PopulateWorkspace
./tools/check-prd-seed.ps1
```

以上命令会写本机演示数据，先校验13306及进程目录、再保存JSON备份；不会清空业务库。seed按日期补未来演示会议，第一次可能追加记录，连续第二次不会重复创建。不要为了刷新页面反复运行完整闭环脚本。

角色演示使用独立的“角色协作演示”团队。首次部署角色代码后准备本机身份，再生成和验收角色数据：

```powershell
./tools/prepare-role-schema.ps1
./tools/seed-role-demo.ps1
./tools/check-role-demo.ps1
./tools/check-role-seed.ps1
```

这些脚本在写入前复用原本机数据库检查；迁移、seed保存完整SQL备份，权限验收保存JSON现场并恢复临时角色、负责人和任务状态。`prepare-role-schema.ps1`仅给本机原admin账号配置系统管理员；seed有同名账号碰撞保护。角色账号、权限表和演示步骤见 `角色权限与协作验收.md`。新注册用户默认为普通用户，由系统管理员加入团队；项目成员需再由项目管理员添加。

第二个脚本使用 `tools/native-runtime/office-check.html` 验证本机 LibreOffice 确实生成 PDF。`check-local.ps1` 是旧版快速检查，会覆盖演示欢迎文档内容，日常优先使用上面两个脚本。

## 已准备的环境

Java 17、Maven 3.9.16、Node 20、MySQL 8.0.43、Redis 5、Nacos 2.4.3、RabbitMQ 3.13.7、Elasticsearch 7.17.27、MinIO、Mailpit 和 LibreOffice 26.2.6。工具主要位于 `tools/`，MySQL 位于 `C:/DevTools/mysql-8.0.43-winx64`。

这是为当前电脑配置的开发环境，复制源码到另一台电脑不会自动安装上述工具。首次准备环境仍需按各组件要求安装、提供 `.env` 并初始化数据库。

邮件仅进入本机 Mailpit <http://127.0.0.1:18025/>，无需付费短信、外部身份提供方或音视频设备。当前界面已移除这些依赖项的入口；历史模块源代码仍保留。

Spring Boot应用日志为 `tools/native-runtime/组件名-app.log`，单文件20MB轮转、历史保留3天、归档总量目标60MB；依赖组件和前端仍查看 `.out.log`/`.err.log` 及各自日志目录。日志采样与历史MQ故障修复见 `运行日志检查-20261016.md`。不要把归档目标当整个项目磁盘占用的绝对上限。

前端资源通过CSP限制本机来源，点击外部链接会提示；ES已关闭GeoIP自动下载。没有配置整机网络防火墙，历史未启用的外部集成源码仍保留。启动器检查端口和网关健康，业务正确性由验收脚本另行检查。本环境仅用于本机演示，不是公网生产部署。
