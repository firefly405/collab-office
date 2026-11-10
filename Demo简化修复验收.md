# Demo 简化修复验收

验收日期：2026-10-16，Batch 17。按“能修就修、不稳定的能力简化”处理上轮发现的问题。下面的结论覆盖本轮列出的用例，不代表完整 PRD 或生产级可靠性全部通过。

## 1. 改好了什么

| 问题 | 状态 | 现在的行为与证据 |
| --- | --- | --- |
| 团队转让后无人能管理 | 已完成 | [事实] 只允许转给团队内另一位启用的系统管理员；普通用户接收请求返回400且所有者不变。转出后原所有者再转让返回403，接收者可以转回。`collab-office-service/user-service/src/main/java/com/collaboffice/user/service/OrganizationService.java:27`；`tools/native-runtime/batch17-permissions-final.log` |
| 持有团队的系统管理员被降级 | 已完成 | [事实] 降级返回409，要求先转让或解散；项目管理员与普通用户之间仍能正常调整。转让与身份调整共用角色行锁。`collab-office-service/user-service/src/main/java/com/collaboffice/user/controller/PlatformRoleController.java:35`、`:41`；`tools/native-runtime/batch17-roles-final.log` |
| 管理操作统计总是0 | 已完成 | [事实] 改读已有团队管理审计；新建部门使今日数、总数各增加1，趋势当日值与今日数相同。仅统计组织、权限、空间设置等管理变更，不冒充所有文档/聊天操作或平台身份修改的总量。`collab-office-service/user-service/src/main/java/com/collaboffice/user/controller/AdminController.java:59`；`tools/native-runtime/batch17-simplification.json` |
| PDF 内嵌区域空白 | 偏差降级 | [事实] 移除云盘PDF/Office等非图片文件的iframe与转换轮询，改为文件详情和下载。PDF经页面按钮实际下载30500字节，与原文件SHA256一致，解析器读取中文正文通过。`collab-office-web/src/views/file/FilePreviewPage.vue:10`；`tools/native-runtime/batch17-pdf-download.json`、`batch17-export-reopen.log` |
| 图片也被转换为PDF | 已完成 | [事实] 图片改用鉴权后的原图下载地址。通过页面上传项目自带hero.png并打开，原图尺寸343×361，加载完成；不再调用转换接口。`collab-office-web/src/views/file/FilePreviewPage.vue:26`；`tools/native-runtime/batch17-browser.json` |
| 不存在的地址产生500和异常堆栈 | 已完成 | [事实] 补充Spring资源不存在异常的404处理；10个业务/WS服务实测错误路径全部404，检查窗口无新增ERROR。`collab-office-common/src/main/java/com/collaboffice/common/handler/GlobalExceptionHandler.java:18`；`tools/native-runtime/batch17-simplification.json` |
| 打包成功却还运行旧公共组件 | 已完成 | [事实] 实测部分未改动服务JAR仍含旧公共组件；父POM强制重建普通JAR后再Boot重打包，重启后10项探针通过。产物检查确认新异常类已包含。`pom.xml:183`；`tools/native-runtime/batch17-package-artifact.json` |

## 2. 简化的决定

- [事实] 保留三角色及其权限边界，团队所有者限定系统管理员。前端转让改成合格管理员选择框，无候选时明确提示；`collab-office-web/src/views/org/OrganizationSettingsPanel.vue:6`、`:33`。本轮浏览器验证无候选分支；成功转让与恢复由真实接口脚本验证，没有冒充浏览器点击完成。
- [事实] 云盘取消非图片在线转换预览，保留下载和图片原图预览；Word/PDF文档导出仍保留。后台旧转换代码暂存，但此页面已不调用，不声称整个后端模块已删除。
- [推断] 对本机演示，下载后打开比继续依赖浏览器PDF内嵌更可控；依据是上轮内嵌空白、本轮真实下载与解析均成功。恢复在线预览时需单独验收浏览器兼容、响应头和权限；原空白的完整原因仍未确认。
- [事实] 没有引入新依赖、付费API或外部服务；没有放宽权限、约束或测试断言。

## 3. 实际验证

| 检查 | 结果与证据 |
| --- | --- |
| 前端TS与生产构建 | [事实] `npm --prefix collab-office-web run build`通过，最终9.81秒；保留已有大包警告。`tools/native-runtime/batch17-build-final.log` |
| 后端完整打包与测试 | [事实] `mvn --batch-mode package`通过，1分36秒；156项中155通过、1项既有产物条件测试跳过，无失败。随后提供真实导出文件目录单独运行ExportFileTest，4项通过，补齐该条件用例；合计156个不同用例覆盖，不重复累计重跑。`batch17-package-final.log`、`batch17-export-reopen.log`，均在`tools/native-runtime/` |
| 团队/部门/自定义角色/继承权限 | [事实] 最终权限脚本通过，包含转让、恢复、解散、评论与到期拒绝。`tools/check-prd-permissions.ps1:54`；`tools/native-runtime/batch17-permissions-final.log` |
| 三角色边界与互动 | [事实] 39项断言通过，含真实身份变更回读、越权拒绝、汇报回复/已阅及群消息读取。`tools/native-runtime/batch17-roles-final.log` |
| 主干协作 | [事实] 34项断言通过，含真实跨账号WS收发、文档回读、上传下载字节、会议事项幂等转任务、表格/导图及搜索。`tools/native-runtime/batch17-collaboration-final.log` |
| 桌面/窄屏 | [事实] 管理统计、转让提示、PDF详情/下载、图片预览分别检查1440×1000与390×843，共8张截图；PDF窄屏页面宽度390，无整页横向溢出。`tools/native-runtime/batch17-browser.json` |
| 日志与运行 | [事实] 最终网关UP、前端HTTP200；20:46:05至20:55:24的11份应用日志新增ERROR为0，保留负向权限测试产生的WARN。`tools/native-runtime/batch17-final-log-health.json`。有限观察不代表永远无异常 |

## 4. 本地数据与失败记录

[事实] 每次写入前沿用守卫校验127.0.0.1:13306、项目MySQL数据目录与实际后端连接。审计测试、图片上传和权限fixture均有写前JSON/SQL备份，见`tools/native-runtime/before-demo-simplification-*`、`before-batch17-image-*`、`before-prd-transfer-*`、`before-demo-acceptance-*`。只追加本地演示验收数据，未删表或清除原业务数据。

[事实] 首次权限复验临时提升旧审阅账号后，因其原有默认团队而无法正常降级；按备份仅恢复该命名fixture的原MEMBER身份，未改其团队/文档。新脚本使用独立测试接收账号，转回后只解散其测试默认团队并恢复MEMBER；保留普通用户测试账号与验收数据，不遗留新增管理员。首次失败保留`batch17-permissions.log`及`before-reviewer-role-recovery.tsv`；后者均在ignored运行目录。

[事实] 首次后端打包遇到运行JAR占用；停应用后重打包。第一次错误路径复验仍失败，进一步确认旧公共依赖滞留，再修POM打包并重启复验通过；保留`batch17-package.log`、`batch17-simplification.log`。启动后数据库连接尚未初始化时守卫曾拒绝验证；只读请求建立实际连接、重新校验通过后才继续写入，没有绕过本机检查。历史失败日志未删除。

## 5. 截图索引

目录：`tools/native-runtime/screenshots/prd-round/batch17/`。

| 页面 | 桌面 | 窄屏 |
| --- | --- | --- |
| 真实管理审计统计 | admin-audit-desktop.png | admin-audit-narrow.png |
| 无合格管理员的转让提示 | transfer-no-admin-desktop.png | transfer-no-admin-narrow.png |
| PDF下载详情 | pdf-download-desktop.png | pdf-download-narrow.png |
| 原图在线预览 | image-preview-desktop.png | image-preview-narrow.png |

## 6. 剩余边界

[事实] 本轮解决上次发现的演示断点，并明确减掉非图片在线预览；不重新引入Android、外部登录/短信、生物识别、音视频、外部设备。完整PRD高级能力仍按`PRD补齐验收.md`的遗留清单处理，没有因这些修复自动完成。

[未知] 本轮没有重新逐个点击全站所有按钮、穷举全部异常组合，未重跑全套seed幂等、外部浏览器兼容、断网合并、磁盘满、长时浸泡、即时撤销既有WS/下载链接；万人连接与100ms性能指标未验证。上轮全站截图与流程记录仍保留在`流程闭环复验.md`，本报告覆盖其中团队转让、统计、文件预览和404处理的旧结论。

本机入口：<http://127.0.0.1:3000/>。三角色演示步骤继续参照`Demo演示说明.md`、`角色权限与协作验收.md`。
