# PRD 补齐验收

最新补充（Batch 17）：已修复三角色版本的团队转让权限断点、管理统计及缺失资源500，云盘非图片预览改为下载；详见 [Demo简化修复验收](Demo简化修复验收.md)。下面保留各批次历史结果，高级PRD遗留与未验证性能边界不变。

需求来源：`../docs/实时协同文档办公平台_PRD.md` 第 4 节。验收日期：2026-10-15 至 2026-10-16。最终范围按用户最新要求收敛为本机 Demo 闭环，复杂高级功能允许减配。下表评价完整 PRD 的覆盖程度；“部分完成”不代表演示主流程不可用，也不等于完整 PRD 已交付。未取得证据的功能不计为完成。

给非技术读者：登录后可以实际创建和编辑内容、沟通、分配任务、安排会议、把纪要事项转为任务，并重新打开查看保存结果。所有服务继续在本机运行；演示步骤见 `Demo演示说明.md`。构建通过、后端 132 项测试通过、完整闭环脚本 33 条断言通过；16 个页面的桌面/窄屏证据见第 5 节。

## 1. 总览表

| PRD 模块 | 状态 | 证据 | 遗留项 |
| --- | --- | --- | --- |
| M1 用户与组织 | 部分完成 | [事实] 本地账号、团队成员、部门、自定义角色、文件夹继承与审计；`collab-office-web/src/views/org/OrganizationSettingsPanel.vue:1`；Batch 9 权限接口实测 | 完整设备管理、找回密码、二次验证及完整 RBAC 未交付 |
| M2 文档 | 部分完成 | [事实] 编辑保存、实时协作、评论/回复/@、版本/快照/比较、大纲、分享撤销、模板与 Word/PDF；`collab-office-web/src/views/doc/DocEditorPage.vue:1`；Batch 2/4 | 离线合并、完整富文本与导出保真、文档内聊天 |
| M3 表格 | 部分完成 | [事实] 公式、格式、合并、排序筛选、图表、导入导出和 WebSocket 同步；`collab-office-web/src/views/sheet/SpreadsheetEditor.vue:1`；Batch 5 | 高级透视表排除；完整 Excel 兼容和大文件性能未交付 |
| M4 导图与白板 | 部分完成 | [事实] 导图拖拽/折叠/样式/三布局/历史/演示/导出；白板画笔、图形、撤销、协同和 PNG；`collab-office-web/src/views/mindmap/MindmapEditorPage.vue:1`、`src/views/whiteboard/WhiteboardEditorPage.vue:1`；Batch 6/11 | 固定画布与操作上限；完整主题历史、模板市场等减配 |
| M5 消息 | 部分完成 | [事实] 真实跨账号收发、已读、撤回、回复、转发、搜索、群公告/成员/置顶/免打扰/文件；`collab-office-web/src/views/im/IMLayout.vue:1`；闭环脚本 | 行内图片/表情、群头像、桌面通知、文档内聊天 |
| M6 会议 | 部分完成 | [事实] 预约/即时创建、改期、取消、邀请、ICS、纪要与事项转任务；`collab-office-web/src/views/meeting/MeetingListPage.vue:1`、`collab-office-service/meeting-service/src/main/java/com/collaboffice/meet/controller/MeetingScheduleController.java:23`；Batch 1/10/11 | 投票等互动与真实出勤统计未做；音视频明确排除 |
| M7 任务项目 | 部分完成 | [事实] 看板拖拽、WIP、子任务/附件/重复、里程碑/成员/归档、日历和报表；`collab-office-web/src/views/task/TaskKanbanPage.vue:1`；Batch 3 | 一层子任务、团队级项目访问；高级日历/提醒减配 |
| M8 文件知识库 | 部分完成 | [事实] 上传下载、预览/移动；知识分类增改删、详情编辑、标签/收藏/贡献；`collab-office-web/src/views/file/KnowledgeListPage.vue:1`；Batch 2/11 | 完整文件版本/分享/回收站界面、知识关联图、审批、模板市场 |
| M9 搜索通知 | 部分完成 | [事实] 多类型搜索、筛选/高亮/建议/历史、命令面板、通知/订阅；`collab-office-web/src/views/search/SearchResultPage.vue:1`；Batch 7/8 | 搜索分页、白板正文检索、跨浏览器搜索历史、通知分页 |
| M10 设置统计 | 部分完成 | [事实] 主题/编辑器/通知偏好、真实统计趋势、报表和 ZIP；`collab-office-web/src/views/settings/WorkspaceStatisticsPage.vue:1`；Batch 8 | 全量翻译/时区、无限量导出、每篇文档 Word/PDF 打包 |

## 2. 清单修正

[事实] 已逐项阅读 PRD 第 4 节（原文第 118 至 887 行）。原批次清单遗漏项纳入如下：

[事实] 范围修正：按 2026-10-16 用户“只对外做 Demo 演示、不进行外部链接”和“复杂高级功能可以减配”的最新指示，主流程继续完成并验收，扩展项转入第 4/7 节。未重新引入已排除的外部能力。白板仍属 M4.2，已实际补齐；分类管理、贡献统计、会议改期及导图拖拽验收已在 Batch 11 补齐，不能继续列为未实现。

- Batch 2：M2.1 文档复制、移动、置顶、标签；M5.1 消息编辑、图片、已读回执、聊天导出；M5.2 群头像、管理员、@、群文件和退出；M5.4 草稿与历史分页；M5.5 会话分组；M5.6 文档内聊天；M8.2 知识标签、收藏与贡献。
- Batch 3：M7.1 标签；M7.2 项目成员；M7.3 自定义列、WIP 与导出；M7.4 周/日视图、日期拖动、冲突；M7.5 提醒配置；M7.6 个人/项目统计。
- Batch 4：M2.2 富文本完整工具核对；M2.3 协作者与仅评论权限；M2.4 评论通知、批注导出；M2.5 自动快照、备注、保留策略；M2.6 导出选项；M2.7 分享有效期、访问统计；M2.8 大纲完整流程。
- Batch 5：M3.2 类型、验证、条件格式和批注；M3.3 函数类别、提示、错误、跨表；M3.5 分组、冻结；M3.6 CSV/PDF 和映射配置。
- Batch 6/11：M4.1 节点备注、链接、图标、PDF；M4.3 操作历史、协作者、视图跟随；M4.4 模板；M4.5 演示模式。白板归属 M4.2，不能因非侧栏主入口而排除。
- Batch 7/8：M9.1 标签筛选、快捷搜索；M9.2 通知分类与跳转；M9.3 动态订阅/汇总；M9.4 最近访问及命令面板；M10.1 语言、快捷键、默认视图；M10.2 配额、安全；M10.4 所有指定数据类型的打包导出。
- Batch 9：M1.1 邮箱账号、找回、刷新、失败锁定和设备上限；M1.2 头像、签名、标签、状态、主页；M1.3 团队资料、邀请、配额、安全；M1.4 成员导入、分组、访客期限；M1.5 继承与审计；M1.6 非短信安全设置。
- Batch 10：M6.1 改期、即时会议、邀请、提醒；M6.3 非音视频互动；M6.5 纪要、导出和统计。

## 3. 自主决策清单

- [事实] 沿用本地账号文件；账号 `admin` 是原演示账号改名，保留 userId=1。报告不记录密码或密钥。
- [事实] 网关健康路径为 `/api/health`，见 `collab-office-gateway/src/main/java/com/collaboffice/gateway/config/HealthConfig.java:16`。四个业务服务没有可用 actuator 健康端点，以网关转发后的实际接口成功返回 code=200 验证，不能把端口监听等同业务健康。
- [事实] 继续采用 ponytail 约定，优先复用现有接口、组件和依赖；本轮高级能力的缩减只依据用户最新授权，逐项记录偏差。
- [事实] Batch 1 采用既有取消态 4。`V3__chat_meeting_task_file.sql:71` 明确 1待开始/2进行中/3已结束/4已取消；保留 V9 约束，不新增 V15，不批改历史业务数据。新预约统一写 1。会议列表和工作台共用 `utils/meeting.ts:1` 过滤函数。
- [事实] Batch 2 使用独立本机验收团队 spaceId=15，避免给原演示团队灌入重复验收记录。写入前复用 seed 的本机数据库检查并保存 JSON；V15/V16 迁移前保存完整 SQL 备份。模板复制沿用文档创建流程，生成独立权限和内容，不把模板直接当可编辑文档。
- [事实] Word/PDF 导出先将编辑器 HTML 转为已有块结构，使用 JDK HTML 解析器，不引入依赖；PDF 使用本机中文字体，可通过 `app.export.font-path` 配置（`ExportServiceImpl.java`）。
- [事实] Batch 3 复用现有看板、附件和任务接口；列移动与任务编辑放在同一事务，锁定列检查 WIP。重复任务完成时只创建一次后继任务。项目成员采用协作名单，项目仍按团队权限访问；子任务限定一层，避免无限嵌套。依据 `collab-office-service/task-service/src/main/java/com/collaboffice/task/service/impl/TaskServiceImpl.java:124`、`service/ProjectService.java:52`。
- [事实] Batch 5 使用既有 Apache POI 统一公式、Excel导入导出，不增加重型依赖；POI缺少的DATEDIF通过官方函数扩展点实现，见 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/SheetWorkbookService.java:16`。原排序仅排序散列单元格且不落库，已改为工作簿锁内整行持久化并移动相对公式引用（`SheetAnalysisService.java:32`）。复用现有WebSocket广播，在HTTP保存成功后通知其他页面读取；未保存格保留本地修改，旧值冲突拒绝覆盖。
- [事实] Batch 6/11 沿用已有 Canvas、SVG、WebSocket 和轮询，不新增绘图库。白板以不可变操作追加、本人撤销/重做实现轻量协同，冲突返回 409，失败操作可重试或备份。固定 1920×1080、单板 2000 操作上限，见 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/WhiteboardWorkspaceService.java:18`、`collab-office-web/src/views/whiteboard/WhiteboardEditorPage.vue:9`。
- [事实] Batch 7/8 搜索沿用 ES 与 SQL 回退；历史保存在本浏览器。统计/导出使用当前用户真实权限过滤，限制条数和体积；不生成虚构趋势。依据各批次对应服务。
- [事实] Batch 9 自定义角色以五项操作权限实现最小可用版；空间解散采用停用并保留数据，不删业务表，见 `collab-office-service/user-service/src/main/java/com/collaboffice/user/service/OrganizationService.java:18`。
- [事实] Batch 10/11 纪要只提取显式事项，不接外部 AI；转任务保留版本检查和幂等。改期保留原受邀人并可追加，不删除出勤历史；导出本地 ICS 含提前 15 分钟提醒，见 `collab-office-service/meeting-service/src/main/java/com/collaboffice/meet/controller/MeetingScheduleController.java:35`、`collab-office-web/src/views/meeting/MeetingListPage.vue:1`。
- [事实] Batch 11 前端资源用 CSP 限制为本机、阻止外部链接点击；ES 关闭不需要的 GeoIP 自动下载。未把浏览器限制描述成操作系统全机断网；旧 OAuth 源码未接入本轮路由。依据 `collab-office-web/index.html:5`、`src/main.ts:19`、`start-local.ps1:139`。
- [事实] 最终验收脚本沿用 seed 原始数据库守卫，先完整 SQL 备份再创建隔离测试数据；补正旧脚本缺少纪要 revision 和错误取消地址，没有放宽断言。取消两次失败尝试留下的独立测试会议前保存 JSON，未删除非演示业务记录。依据 `tools/check-collaboration.ps1:3` 及 `tools/native-runtime/before-demo-test-meeting-cleanup-*.json`。
- [事实] 收尾发现评论通知被通配队列和提及队列同时处理。保留现有队列绑定，让通用监听器跳过由提及监听器拥有的routing key；避免改动已存在队列/历史记录。依据 `collab-office-service/notification-service/src/main/java/com/collaboffice/notification/listener/NotificationListener.java:36`；新增路由测试和真实评论链路验收后仅重启通知服务。

## 4. 偏差清单

- 未完成：[事实] 按用户明确边界排除 Android、第三方登录、短信、生物识别、音视频和外部设备，不重新引入。
- 未完成：[事实] 离线编辑闭环合并、高级透视表、PRD 5.1 性能达标验证列入遗留，不声称通过。
- 部分完成：[事实] 模板复制四类实际内容，导图布局/主题已复制；表格图表/合并等元数据仍非全量复制，见 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/TemplateService.java:49`。Word/PDF 已验证标题、中文正文，行内样式和图片保真未完成。知识库纯文本编辑；分类增改删和团队贡献统计已在 Batch 11 补齐，但页头“我的贡献”仍按当前加载页计数。群头像、行内图片/表情、会话隐藏/清空、文档内聊天未完成；旧无 space_id 群暂不允许跨团队邀请，见 `collab-office-web/src/views/file/KnowledgeListPage.vue:3`、`src/views/im/IMLayout.vue:1`。
- 偏差降级：[事实] 表格是有上限的轻量工作簿，条件格式只支持单阈值、冻结首行首列；高级透视表排除。协同已接通 WebSocket，断连时保存加轮询读取，不承诺离线合并；具体上限见 Batch 5。
- 偏差降级：[事实] 白板固定画布，撤销只影响本人操作，历史界面最近 100 条、总操作最多 2000，导出位图；无无限画布/复杂对象组合。导图历史只恢复节点、导出图标简化；已验证真实拖拽及窄屏自动适应。依据 `WhiteboardWorkspaceService.java:28`、`collab-office-web/src/views/whiteboard/WhiteboardEditorPage.vue:10`、Batch 6/11。
- 偏差降级：[事实] 项目成员为团队协作名单，一层子任务；角色为五项权限上限，非独立通用 RBAC 引擎。已发下载地址有效期和已建立 WebSocket 的即时撤权不保证；见 Batch 3/9。
- 偏差降级：[事实] 会议演示采用预约、邀请、纪要、任务和本地 ICS，不接音视频或外部日历。统计为计划时长与受邀人数，不是实际出勤；改期追加成员而非移除原成员。见 Batch 10/11。
- 偏差降级：[事实] 搜索每类最多 100 条，消息采用 SQL 文本检索，白板只搜标题；统计固定 14 天，ZIP 文档为 JSON，不是逐篇 Word/PDF；语言/时区未全站适配。见 Batch 7/8。

## 5. 回归结果

### Batch 0 基线

- 已完成：[事实] 执行 seed 原有 `Assert-LocalDemoDatabase` 检查：MySQL 仅监听 `127.0.0.1:13306`，进程使用项目 `tools/native-runtime/mysql-data`；8101/8102/8105/8106 的数据库连接均为本机 13306。已建立的 Redis、RabbitMQ、Elasticsearch 连接均为 127.0.0.1。
- 已完成：[事实] 前端 HTTP 200；`admin` 登录 code=200；`/user/info`、`/document/list?spaceId=13&pageSize=1`、`/meeting/list`、`/task/list` 经前端代理和网关返回 code=200；网关 `/api/health` 返回 UP。
- 已完成：[事实] `collab-office-web` 执行 `npm run build` 成功，包含 vue-tsc；日志 `tools/native-runtime/prd-batch0-build.log`。保留已有包体积和 Sass 警告。
- 已完成：[事实] 1440x1000 和 390x844 实际浏览器检查工作台。窄屏根节点 scrollWidth=390；卡片为文档9、待办3、会议3。较早截图会议4中的一场已自然结束，非数据丢失。
- 未完成：[事实] 本批无后端改动，未执行后端测试；未运行 seed、未写业务数据。

### Batch 1 会议取消

- 已完成：[事实] `MeetingRoom.java:14` 定义既有四种状态；`MeetingServiceImpl.java:154` 取消写 4，非主持人拒绝；`MeetingController.java:48` 预约写 1。前端列表、按钮、工作台统计与 seed 统一取消态 4。
- 已完成：[事实] `mvn --batch-mode -pl collab-office-service/meeting-service -am test` 成功；会议服务 9 项测试、0失败0错误，含新取消幂等及非主持人测试。日志 `tools/native-runtime/prd-batch1-test.log`。
- 已完成：[事实] `npm run build` 与服务打包成功，日志 `prd-batch1-build.log`、`prd-batch1-package.log`；仅重启会议服务，其他服务保持运行。
- 已完成：[事实] `tools/check-prd-meeting.ps1` 复用 seed 登录和本机保护；修改前保存全部可见会议 JSON。接口实测预约1、取消4、重复取消成功；证据 `tools/native-runtime/prd-batch1-meeting.json`。桌面和窄屏实测可加载、筛选已取消记录；窄屏宽表在自身区域横向滚动。
- 已完成：[事实] `./tools/check-prd-meeting.ps1 -Cleanup` 备份后只软删除本次新建测试会议，原有会议保留。没有修改已有会议日期或内容。
- [事实] 基线提交 `285af25`；Batch 1 提交信息 `batch-1: 统一会议取消状态并验证筛选与权限`。

### Batch 2 内容与消息入口

- 已完成：[事实] 模板中心和知识库接入路由及侧栏；`collab-office-web/src/views/doc/TemplateCenter.vue:1`、`src/views/file/KnowledgeListPage.vue:1`。模板使用生成新文档并复制实际富文本、单元格、节点、白板元素：`document-service/.../service/TemplateService.java:70`；知识库分类、增改详情、标签、收藏与作者权限已实测。
- 已完成：[事实] 文档复制、根目录/文件夹移动、标签、个人置顶可用（`src/views/doc/DocListPage.vue:1`）；新增分享记录及撤销入口（`src/views/doc/DocEditorPage.vue:120`），API 实测撤销后匿名访问404，列表不返回密码哈希。
- 已完成：[事实] 群公告、成员管理/管理员/转让/退出、置顶/免打扰/分组/草稿、群文件、消息引用/撤回/编辑/转发/搜索/已读/分页/导出接通：`message-service/.../controller/ChatManagementController.java:16`、`src/views/im/IMLayout.vue:1`。撤回仅本人两分钟内允许，所有消息读取入口遮蔽已撤回正文。
- 已完成：[事实] `tools/check-prd-content.ps1`、`check-prd-organization.ps1` 接口验证成功；证据 `tools/native-runtime/prd-batch2-content.json`、`prd-batch2-organization.json`，包含四类内容复制、收藏、文件夹移动、标签、群置顶等断言。只写本机13306；迁移备份 `before-v15-20261015-160244.sql`、`before-v16-*.sql`，业务备份 `before-prd-*.json`。
- 已完成：[事实] 前端 `npm run build` 含 TS 检查成功；相关服务 Maven 测试 common6/document9/message7/file6 共28项0失败0错误0跳过；日志 `prd-batch2-build.log`、`prd-batch2-test.log`。`ExportFileTest.java:12` 重新打开接口下载的 Word/PDF 并检查中文，原始产物 `prd-batch2-export.docx`、`prd-batch2-export.pdf`。
- 已完成：[事实] 浏览器1440/390实际查看模板、知识库详情/编辑/收藏、聊天/群设置/搜索、文档操作、分享和导出；Word/PDF 按钮点击后弹窗正常关闭，控制台无错误。截图均在 `screenshots/prd-round/batch2/`；窄屏内容和弹窗未溢出根页面，宽文档表格在表格区域滚动。
- 部分完成：[事实] 本批交付不等于 M2/M5/M8 全部完成，未覆盖细节见偏差及遗留列表。

### Batch 3 任务与项目

- 已完成：[事实] 看板原生拖拽、自定义列/WIP/颜色/顺序、子任务、标签、真实附件、依赖与循环拒绝、每日/周/月重复任务已接通；入口 `collab-office-web/src/views/task/TaskKanbanPage.vue:1`，后端 `collab-office-service/task-service/src/main/java/com/collaboffice/task/service/impl/TaskServiceImpl.java:124`、`service/impl/KanbanServiceImpl.java:1`。跨列移动失败会回滚同次任务编辑。
- 已完成：[事实] 项目成员、里程碑、归档/恢复、个人/项目统计、14天趋势与 CSV 报表：`collab-office-service/task-service/src/main/java/com/collaboffice/task/service/ProjectService.java:42`、`:52`、`:85`。月/周/日日历、日期拖动及同人同日冲突提示已接入。报表浏览器实际下载后检查总数4、完成3、完成率75%及趋势日期。
- 已完成：[事实] 提醒启用/提前天数/频率使用 `collab-office-service/task-service/src/main/java/com/collaboffice/task/service/TaskSchedule.java:28`；移除重复自动调度，17:00 任务日志记录两条提醒入队。未将队列入队当作用户收件成功。
- 已完成：[事实] 再次执行 seed 本机保护，V17 前完整 SQL 备份、接口写入前 JSON 备份。`tools/check-prd-tasks.ps1` 验证真实列持久化、WIP拒绝和事务回滚、同列幂等、依赖循环、附件、重复任务幂等、归档拒写与恢复；结果 `tools/native-runtime/prd-batch3-tasks.json`。
- 已完成：[事实] `npm run build`（含 TS）成功；`mvn --batch-mode -pl collab-office-service/task-service -am test` 共 common6/task20=26项，0失败0错误0跳过；服务打包并仅重启 task-service。日志 `tools/native-runtime/prd-batch3-build.log`、`prd-batch3-test.log`、`prd-batch3-package.log`。
- 已完成：[事实] 浏览器1440/390验证看板、任务保存、项目设置保存、统计、日历；桌面实际拖拽状态及日期后恢复日期。窄屏根宽390，周日历仅自身横向滚动。截图目录 `tools/native-runtime/screenshots/prd-round/batch3/`。
- 部分完成：[事实] WIP仅用于自定义列；统计窗口固定14天且包含子任务，看板只列顶层任务。邮件/推送渠道选择、外部日历同步未完成，未引入外部服务。成员名单不构成独立项目 ACL。证据见上述服务及页面。

### Batch 4 评论、版本与大纲

- 已完成：[事实] 选区批注、嵌套回复、@成员、整线程状态筛选、解决与导出；权限4定义为仅评论，禁止正文写入。后端 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/impl/CommentServiceImpl.java:34`；递归界面 `collab-office-web/src/views/doc/components/CommentThread.vue:1`。浏览器点击引用后实际选中“项目计划”，窄屏回复保存成功。
- 已完成：[事实] 版本命名/备注、手动即时保存快照、自动快照、文本差异与保留策略：`collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/DocumentHistoryService.java:18`、`:37`、`:64`。大纲导航、一级筛选、Markdown导出和章节拖动已接通；桌面实际拖动后撤销恢复顺序。
- 已完成：[事实] 修复浏览器以 JSON 字符串包装 HTML 的保存问题，发送 `text/html`，兼容解码历史包装正文。导出可包含评论和最近100版；浏览器下载 `Downloads/PRD 验收 · 评论与版本.txt`（2075字节），检查包含正文、窄屏回复及命名快照。接口证据 `tools/native-runtime/prd-batch4-discussion.json`、`prd-batch4-appendices.txt`。
- 已完成：[事实] V18 本机守卫和 SQL 备份后应用；`npm run build` 含 TS 成功；document/collaboration 及 common 共38测试，0失败0错误0跳过，日志 `tools/native-runtime/prd-batch4-build.log`、`prd-batch4-test.log`。仅重启关联服务。
- 部分完成：[事实] 保留策略只隐藏超限未命名版本，不回收存储，见 `DocumentHistoryService.java:67`；命名版保留。差异输入超过100万行对组合时退化为整段替换；界面仅加载100版。评论通知已入既有发送链路，但其他账号收件未实测。仅评论账号将在权限批次进一步实测。M2富文本扩展、文档聊天、完整导出样式仍未完成，不能据此认定整个M2完成。

### Batch 5 在线表格

- 已完成：[事实] POI公式计算包含PRD指定基础/逻辑/文本/日期/查找函数、跨表引用、错误值显示；工作表改名更新公式引用。格式支持字体、颜色、背景、边框、对齐、合并拆分、类型、数字上下限、下拉验证、阈值条件格式和批注。代码 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/SheetWorkbookService.java:31`、`:47`、`:76`，界面 `collab-office-web/src/views/sheet/SpreadsheetEditor.vue:1`。
- 已完成：[事实] 多列排序、筛选、柱/线/饼/散点图、随数据刷新、基础分组数量/求和/平均、冻结首行首列；图表ID与工作表归属校验，拒绝跨文档修改。代码 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/SheetAnalysisService.java:26`、`:32`；绘图 `collab-office-web/src/views/sheet/SheetChartCanvas.vue:1`。浏览器实测升序150/300后恢复降序800/1600，合并F3:G3后colspan=2、拆分恢复。
- 已完成：[事实] CSV（含逗号及引号）、XLS、XLSX真实multipart导入；Excel、CSV、中文PDF导出；CSV/PDF及导入提供起始行/列映射。代码 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/SheetTransferService.java:37`、`:82`。接口证据 `tools/native-runtime/prd-batch5-sheets.json`：旧值409、无效类型400、跨表总额1950、排序公式1600。Excel/PDF产物重新打开检查中文及公式/合并区域；窄屏浏览器实际下载 `Downloads/PRD 验收 · 团队预算.xlsx`（4573字节）。
- 已完成：[事实] 两个真实浏览器页面实测WebSocket联动：预算800改820，总额1950改1970，另一页恢复800后原页自动显示1950；协作者选区、编辑中标识可见。自动保存失败保留本地并提供复制备份/重新载入；接口按旧内容和格式进行冲突检查。代码 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/impl/CellServiceImpl.java:28`、`collab-office-web/src/views/sheet/SpreadsheetEditor.vue:58`。
- 已完成：[事实] 写入前再次执行本机13306守卫并保存JSON；V19/V20前完整SQL备份。V20修复实体update_time与实际表不一致，并扩展公式/结果字段避免截断。前端 `npm run build` 含TS通过；Maven common7/document23共30项，0失败0错误0跳过；日志 `tools/native-runtime/prd-batch5-build.log`、`prd-batch5-test.log`。重启后再次读取预算800/1600和总额1950成功。
- 部分完成：[事实] 窄屏实测390且根页面scrollWidth=390；图表/格式弹窗修复固定宽度裁切。浏览器文件上传受扩展“Allow access to file URLs”关闭阻挡，未完成浏览器上传提交，未将接口成功冒充浏览器通过。文件选择入口、参数界面已目检；真实multipart接口三种格式均已通过。
- 部分完成：[事实] 本地版限制10000行/256列、导入20000非空格/20工作表/5MB，单次写入及排序10000格；每页100行，图表最多绘制100点、图例12项。条件格式为单个大于阈值，冻结仅首行/首列，非完整Excel规则引擎；重排映射导入将公式转计算值以避免错误引用。XLS样式及图表的文件保真未完成；Excel导出全工作簿，列映射用于CSV/PDF，PDF最多20列。基础分组汇总已做，高级透视表明确排除。仅已连通页面可接收实时广播，断连时自动保存+定时读取，不提供离线合并闭环。

### Batch 6 思维导图

- 已完成：[事实] 节点内容、样式、字体、连接线、折叠、图标、标签、备注与链接，三种布局及布局内位置保存已接通：`collab-office-web/src/views/mindmap/MindmapEditorPage.vue:36`、`src/views/mindmap/geometry.ts:4`。属性编辑保留打开时原值，后端409拒绝陈旧覆盖；修改上级拒绝跨图、循环、第二根、超过50层，单图最多1000节点。服务 `collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/MindmapWorkspaceService.java:23`、`service/impl/MindmapServiceImpl.java:126`。
- 已完成：[事实] 操作前快照、最近100次历史恢复；修复同秒快照无稳定顺序，`mapper/MindmapSnapshotMapper.java:12`。恢复前也保存当前状态。模板复制节点全部属性及布局/主题，沿用团队模板和官方公共模板；`service/TemplateService.java:49`。
- 已完成：[事实] 协作者节点/加入离开提示、视图跟随、服务端演示状态、逐节点推进、演示者权限与标注：`MindmapWorkspaceService.java:48`。真实双浏览器页面跟随到2/5，窄屏展示2个节点；PNG下载90126字节，PDF下载40733字节，后端测试重新打开PDF确认1页和资源。Canvas导出保持节点正文/颜色/连接线/标签，图标简化为标记，非矢量输出。
- 已完成：[事实] 本机13306守卫后备份再建独立导图；`tools/check-prd-mindmap.ps1:7` 实测属性移动、409冲突、400循环、8条快照、5节点演示、模板内容复制及PDF，结果 `tools/native-runtime/prd-batch6-mindmap.json`。前端 `npm run build` 含TS通过；Maven common7/document28共35项，0失败0错误0跳过；`node tools/check-mindmap-geometry.mjs` 通过，日志 `prd-batch6-build.log`、`prd-batch6-test.log`。
- 部分完成：[事实] 桌面1440和窄屏390属性/折叠/布局/演示/导出已目检，窄屏根scrollWidth390。Batch 11 补充了真实指针拖拽及刷新后位置持久化验证，随后恢复原位置；窄屏首开自动缩放为27%，见 `tools/native-runtime/prd-batch11-mindmap-drag.json`、`screenshots/prd-round/batch11/final-mindmap-narrow.png`。图标导出简化、历史只恢复节点不恢复布局/主题；自定义模板仅团队共享，未开放全局公开发布。两个导图协同页面使用同账号；其他成员演示控制拒绝由单元测试验证，未以第二账号实测。白板见Batch11。

### Batch 7 搜索增强

- 已完成：[事实] 全局文档/表格/导图/白板/任务/消息/成员范围、起止日期、创建者和标签筛选；Elasticsearch匹配合并当前数据库内容，并重新核对团队及文档权限。消息限定当前团队、会话成员、正常会话、未撤回文本。表格内容及导图节点补数据库检索：`collab-office-service/search-service/src/main/java/com/collaboffice/search/service/SearchQueryService.java:16`。接口 `tools/check-prd-search.ps1:4` 验证30条总结果、日期/作者、消息、表格内容、错误日期400及跨团队拒绝；证据 `tools/native-runtime/prd-batch7-search.json`。
- 已完成：[事实] 纯文本安全高亮、搜索历史（每账号/团队本地最多12条，可清空）、300ms建议、消息定位到会话及具体消息：`collab-office-web/src/views/search/SearchResultPage.vue:23`、`src/views/search/highlight.ts:1`、`src/views/im/IMLayout.vue:75`。Ctrl/Cmd+K和图标入口打开快捷面板，支持搜索、新建、任务、收藏和本机最近访问：`src/layouts/MainLayout.vue:43`、`src/views/doc/DocListPage.vue:72`。浏览器实际消息跳转成功，窄屏组合日期/类型/标签结果2条；刷新保留搜索历史，建议DOM实际5条。
- 已完成：[事实] 前端构建含TS通过，高亮纯函数自检通过；Maven common7/document28/search9共44项0失败0错误0跳过，日志 `tools/native-runtime/prd-batch7-build.log`、`prd-batch7-test.log`。相关服务打包重启。桌面1440、窄屏390实测，根scrollWidth390，快捷弹窗与筛选控件可用。
- 偏差降级：[事实] 旧文档create_time为空，搜索按最早版本edit_time回退，不批改历史数据库；响应firstVersionTime标识该情况，代码 `SearchQueryService.java:24`。新文档补写创建/更新时间，`collab-office-service/document-service/src/main/java/com/collaboffice/doc/service/impl/DocumentServiceImpl.java:61`。每类最多100条，无搜索分页；消息全文为SQL回退文本检索，未建消息ES索引。搜索历史/最近访问只在当前浏览器持久化；消息不支持标签，带标签筛选时不会返回消息。白板当前仅标题检索，正文检索列入减配。

### Batch 8 设置、订阅、统计与导出

- 已完成：[事实] 主题亮/暗/系统、编辑器字体字号行距宽度、默认登录视图、命令面板快捷键、四类通知偏好读写并应用；`collab-office-web/src/utils/preferences.ts:1`、`collab-office-service/user-service/src/main/java/com/collaboffice/user/service/PreferenceService.java:10`。桌面实际保存暗色、刷新后读取，再恢复亮色；设置窄屏根宽390。
- 已完成：[事实] 当前团队/个人真实文档数、云盘大小、任务完成数、14天编辑次数与新增文件量；`collab-office-service/user-service/src/main/java/com/collaboffice/user/service/WorkspaceDataService.java:28`。修复完成状态口径为2，接口与任务列表一致3/4。路由及侧栏 `/statistics`，浏览器1440/390目检，窄屏main宽321且scrollWidth321；趋势图独立横向滚动。
- 已完成：[事实] ZIP含当前权限可见文档正文/表格单元格/导图节点/白板元素JSON、云盘原文件、本人可见会话未撤回记录、任务JSON及manifest；`WorkspaceDataService.java:39`。真实接口产物33条ZIP成员、52324字节，含1个原文件；浏览器实际下载`Downloads/工作空间数据.zip`（21551字节，仅勾选文档和任务）。证据`tools/check-prd-preferences.ps1:1`、`tools/native-runtime/prd-batch8-preferences.json`。
- 已完成：[事实] 文档/项目/任务订阅增删和频率；每60秒检查内容签名，通知分类关闭阻止写入和推送。`collab-office-service/notification-service/src/main/java/com/collaboffice/notification/service/SubscriptionService.java:15`、`service/impl/NotificationServiceImpl.java:75`。实际更名独立验收文档收到订阅动态，随后恢复标题，证据`tools/native-runtime/prd-batch8-subscription-notification.json`，通知中心1440/390可见。新增通知删除与文档类型跳转。
- 已完成：[事实] V21前本机13306守卫和完整SQL备份；偏好写前JSON备份且接口检查后恢复。前端构建含TS成功，common7/user8/notification3共18项0失败0错误0跳过；日志`tools/native-runtime/prd-batch8-build.log`、`prd-batch8-test.log`。
- 偏差降级：[事实] 英语仅切换ElementPlus控件，业务文案未全量翻译；时区存储未全站应用。编辑器偏好作用默认呈现，不重写已有内联格式。统计活动按正文版本计数，存储趋势为现存文件按创建日新增量，不是历史净占用。ZIP文档为结构化JSON而非每篇Word/PDF，最多5000项/100MB云盘/150MB压缩包/10000条消息。每日汇总按北京时间17:00，尚未等待真实调度；单机轮询最多2000订阅，未验证高负载和多实例去重。通知列表仍最多50条，无分页。

### Batch 9 组织与权限

- 已完成：[事实] 部门树、负责人、成员归属、Excel批量加入已注册账号，自定义角色五项权限、临时权限、空间资料/配额/转让/停用解散及最近100条权限审计接通：`collab-office-service/user-service/src/main/java/com/collaboffice/user/service/OrganizationService.java:18`、`collab-office-web/src/views/org/OrganizationSettingsPanel.vue:1`。V22前执行本机13306守卫并完整SQL备份。
- 已完成：[事实] 文档成员查看/编辑/管理/仅评论和文件夹最近上级ACL继承；列表、搜索、导出及文件移动目标权限校验，配额超限拒绝上传。`collab-office-common/src/main/java/com/collaboffice/common/security/WorkspaceAccess.java:55`、`:116`，文档分享弹窗与云盘文件夹入口均可打开。隐藏文件不计入使用统计，新增`WorkspaceVisibilityTest.java:13`。
- 已完成：[事实] `tools/check-prd-permissions.ps1:1` 实际第二账号验证仅评论成功而改正文403、只读导出403、继承ACL、角色在用删除409、到期403、部门循环400、转让再转回、停用空间文档403及匿名分享404；结果`tools/native-runtime/prd-batch9-permissions.json`。只写独立验收团队17、18，保留原业务数据。
- 已完成：[事实] 前端含TS构建通过；全仓118项测试0失败0错误0跳过，另common10/user11权限可见性复核通过；日志`prd-batch9-build.log`、`prd-batch9-test.log`、`prd-batch9-visibility-test.log`。服务重新打包启动，部门/角色/空间设置/文档权限/文件夹权限1440与390实际目检，截图见batch9。
- 偏差降级：[事实] 自定义角色采用五操作权限上限，覆盖文档/文件及沿用空间读写的模块；未建立独立通用RBAC引擎。项目成员仍为团队内协作名单，既存会话及已建立WebSocket不保证撤权立即断开；已发MinIO下载地址仍按原1小时有效期。空间安全水印/全局下载开关未完成；Excel仅批量加入已有账号，无弱密码开户。文档可见性分页先取候选再权限过滤，存在全量扫描及N+1成本，未验证规模性能。

### Batch 10 纪要与任务联动

- 已完成：[事实] 纪要待办增删、显式待办行提取、勾选、负责人和截止日期、项目选择、转换后任务跳转：`collab-office-web/src/views/meeting/MeetingListPage.vue:4`、`:28`。提取识别`- [ ]`与`待办：`，不调用外部AI；事项上限100，正文20000字。
- 已完成：[事实] 复用`TaskService.createTask/updateTask`，同库事务锁会议行，任务ID写回事项，重复转换不新建；`collab-office-service/task-service/src/main/java/com/collaboffice/task/service/MeetingTaskService.java:15`。保存纪要使用摘要版本拒绝旧覆盖，禁止伪造任务关联、删除已转任务关联或无效成员；`MeetingController.java:97`、`collab-office-common/src/main/java/com/collaboffice/common/util/MeetingTodos.java:9`。
- 已完成：[事实] 本机13306守卫及JSON备份后建独立测试会议；`tools/check-prd-minutes.ps1:1`实测同事项两次任务ID一致，负责人和日期落库，旧纪要409；结果`tools/native-runtime/prd-batch10-minutes.json`。浏览器桌面实际将第二事项转任务并打开详情，窄屏提取第三事项保存成功。
- 已完成：[事实] 计划时长/受邀人数与JSON导出已接通，浏览器下载`Downloads/PRD 验收 · 纪要转任务-会议记录.json`1250字节，解析含3事项、2任务关联、45分钟；未将计划时长冒充实际参会时长。前端含TS构建通过；common12/meeting9/task20测试及新增幂等复核common12/task21均0失败0错误0跳过；日志`prd-batch10-build.log`、`prd-batch10-test.log`、`prd-batch10-idempotency-test.log`。
- 部分完成：[事实] 本批实现非AI纪要记录与明确待办提取，暂无自由文本语义推断。Batch11已补改期、即时会议、本地会议链接和ICS日历文件；外部邮件/日历联动不纳入本机演示，投票等互动未完成；录制回放属于音视频排除范围。关联后在任务页独立维护状态，纪要不反向改写任务。

### Batch 11 本机 Demo 收尾

- 已完成：[事实] 白板归属 PRD M4.2，补画笔、橡皮、矩形/圆/三角/线/箭头/文字、颜色线宽、缩放平移、背景、历史、本人撤销/重做、可撤销清空、模板、PNG、协作者光标/跟随和实时刷新；入口 `collab-office-web/src/views/whiteboard/WhiteboardEditorPage.vue:1`，API `collab-office-service/document-service/src/main/java/com/collaboffice/doc/controller/WhiteboardWorkspaceController.java:11`。校验、权限、操作幂等及冲突见 `service/WhiteboardWorkspaceService.java:18`、`:38`。
- 已完成：[事实] 实际白板绘制后第二页收到更新；撤销/重做、跟随和清空再撤销可用。PNG 已下载并解码，1920×1080、68513字节、采样888个有色像素，非空白文件。接口重复操作不新增、不同内容同标识409、非法坐标400。证据 `tools/native-runtime/prd-batch11-whiteboard.json`、`prd-batch11-final.json`、`screenshots/prd-round/batch11/whiteboard-history-desktop.png` 与 `whiteboard-clear-desktop.png`。
- 已完成：[事实] 会议主持人改期、时间校验、取消/结束禁止改期、追加邀请及站内通知；即时创建、本机邀请链接、ICS含15分钟提醒已接通。接口实测改期后恢复原日期；桌面/窄屏弹窗与ICS下载已验证。证据 `collab-office-service/meeting-service/src/main/java/com/collaboffice/meet/controller/MeetingScheduleController.java:23`、`tools/native-runtime/prd-batch11-final.json`、`screenshots/prd-round/batch11/schedule-desktop.png`、`schedule-narrow.png`。
- 已完成：[事实] 知识分类重命名/删除和团队贡献统计接口及弹窗补齐；独立临时分类增改删实测，未删除原分类；贡献统计显示2条目、3浏览、2收藏。依据 `collab-office-service/file-service/src/main/java/com/collaboffice/file/controller/KnowledgeManagementController.java:33`、`:41`、`:51`、`tools/native-runtime/prd-batch11-final.json`。
- 已完成：[事实] 修复导图首开适应画布与鱼骨布局越界；真实拖拽后刷新读取位置成功，再恢复原位置。证据 `collab-office-web/src/views/mindmap/MindmapEditorPage.vue:26`、`src/views/mindmap/geometry.ts:4`、`tools/native-runtime/prd-batch11-mindmap-drag.json`。最终窄屏5个节点均处于画布范围，缩放27%。
- 已完成：[事实] 修复过期凭证刷新返回异常导致前端 TypeError；并发失败仅刷新一次、清除凭证后回登录页，无循环重试。实测使用真实 Axios 和本机后台，浏览器存储/提示以脚手架提供；未替换被测拦截器或服务。依据 `collab-office-web/src/utils/token.ts:4`、`src/utils/request.ts:64`、`tools/check-auth-refresh.mjs:1`。
- 已完成：[事实] 历史 MQ 不可变集合消息可被正确反序列化，未知反序列化类型拒绝重投；Spring Boot 日志按20MB轮转、归档目标60MB、保留3天，不再写无限增长的console重定向文件。依据 `collab-office-common/src/main/java/com/collaboffice/common/config/MessageConversionConfig.java:36`、`RabbitListenerResilienceConfig.java:15`、`start-local.ps1:163`。13次/6分钟采样未复发；最终全站检查后仍未出现重投异常。完整限制与历史错误见 `运行日志检查-20261016.md`。
- 已完成：[事实] 浏览器资源限制为本机，外部锚点阻止跳转；纯函数检查6种合法/8种拒绝输入。ES的GeoIP下载有效设置为false。最终页面DOM未发现外部src元素，未出现CSP阻断业务的控制台错误；这不是整机网络隔离证明。依据 `collab-office-web/index.html:5`、`src/utils/local-demo.ts:1`、`tools/check-local-demo-links.mjs:1`、`start-local.ps1:139`。
- 已完成：[事实] 最终前端 `npm run build` 含 vue-tsc 成功；补重复通知修复后全仓 `mvn --batch-mode test "-Dprd.export.directory=C:/Users/AR-26710/Desktop/1-demo/1-demo/collab-office/tools/native-runtime"` 共132测试、0失败/0错误/0跳过。普通test命令会跳过1项需真实产物的测试，已补参数重跑；未修改测试条件或断言。几何、刷新响应、本机链接自检成功。日志 `tools/native-runtime/prd-batch11-demo-build.log`、`prd-batch11-demo-test.log`。此前功能打包见 `prd-batch11-final-package.log`；运行中jar已于16日重建，通知补丁打包见 `prd-batch11-demo-notification-package.log`，最终部署状态见 `prd-batch11-runtime-final.json`。未新增独立前端单元测试框架。
- 已完成：[事实] 修复同一评论因`notification.#`和`notification.mention`两个绑定生成重复通知。新增单元测试验证两监听器合计只调用一次落库，且会议邀请继续走通用处理器；真实第二账号评论通过MQ后连续20次观察只收到1条通知，随后清理本次临时评论/通知，旧记录保留。证据 `collab-office-service/notification-service/src/test/java/com/collaboffice/notification/listener/NotificationRoutingTest.java:18`、`tools/check-prd-notifications.ps1:1`、`tools/native-runtime/prd-batch11-notification-dedup.json`、`prd-batch11-demo-notifications.log`。本修复解决双绑定重复，不宣称所有MQ重复投递均已业务幂等。
- 已完成：[事实] `tools/check-collaboration.ps1` 33条断言全部通过：真实跨账号WebSocket、文档保存/版本/评论/权限、文件字节比对、任务、会议、事项重复转任务同ID、表格/导图保存、导出、分享/回收站、搜索/通知等。证据 `tools/native-runtime/prd-batch11-demo-acceptance.log`。`tools/check-prd-final.ps1` 复查会议改期恢复、分类生命周期、贡献统计和白板冲突通过，证据 `prd-batch11-demo-final-api.log`。
- 已完成：[事实] 所有数据库写入前均执行既有本机13306守卫，完整验收有SQL备份；seed有全工作区JSON备份。`tools/check-prd-seed.ps1` 连跑两次，文档/任务/文件/会议ID集合不变。首轮按日期补未来演示会议，第二轮未重复创建；证据 `tools/native-runtime/prd-batch11-seed.json`、`prd-batch11-demo-seed.log`、`before-demo-acceptance-*.sql`、`before-dashboard-workspace-*.json`。
- 已完成：[事实] 统计卡来自接口结果：文档取接口total，任务与会议取接口列表后同一计算属性筛选，并用于卡片与下方列表。最终9文档、3待办、5会议一致，取消会议不计入；会议是“我的日程”，跨所选团队仍可显示，不是团队会议数。依据 `collab-office-web/src/views/doc/DocListPage.vue:75`、`:89`、`:97`、`tools/native-runtime/screenshots/prd-round/batch11/final-regression.json`。真实空团队验证任务空状态及创建入口，无需改动演示任务。

### 最终页面回归表

[事实] 2026-10-16，实际浏览器1440×1000和390×844。每项均验证内容载入、标题/按钮未互相遮挡、入口可见、根页面没有横向溢出；宽表、工具栏、图表允许在自身容器滚动。此处“已完成”仅指页面目检，交互/API覆盖以各批次记录为准，不等同所有组合操作全测。

全部截图位于 `tools/native-runtime/screenshots/prd-round/batch11/`，表格中给出文件前缀；后缀为 `-desktop.png`、`-narrow.png`。DOM宽度、实际路由、页面文字和控制台记录见同目录 `final-regression.json`。

| 页面 | 桌面 | 窄屏 | 截图前缀 / 实际检查 |
| --- | --- | --- | --- |
| 工作台 | 已完成 | 已完成 | `final-dashboard`；9/3/5与列表一致 |
| 云文档 | 已完成 | 已完成 | `final-documents`；6篇文字文档，类型筛选与工作台9份全部类型不同 |
| 文档编辑器 | 已完成 | 已完成 | `final-editor`；项目计划正文实际载入 |
| 表格 | 已完成 | 已完成 | `final-sheets`；预算/汇总与真实单元格，表内滚动 |
| 思维导图 | 已完成 | 已完成 | `final-mindmap`；5节点，窄屏自动适应 |
| 白板 | 已完成 | 已完成 | `final-whiteboard`；真实图形，7有效操作 |
| 任务 | 已完成 | 已完成 | `final-tasks`；4顶层任务、3待办、状态分组 |
| 云盘 | 已完成 | 已完成 | `final-files`；2原文件可见 |
| 会议 | 已完成 | 已完成 | `final-meetings`；5待开/进行中，取消过滤 |
| 消息 | 已完成 | 已完成 | `final-messages`；公告、置顶、撤回与回复正文 |
| 搜索 | 已完成 | 已完成 | `final-search`；PRD查询35结果、筛选/高亮 |
| 设置 | 已完成 | 已完成 | `final-settings`；主题/编辑器/通知偏好 |
| 团队管理 | 已完成 | 已完成 | `final-team`；成员及组织权限标签 |
| 模板中心 | 已完成 | 已完成 | `final-templates`；PRD验收团队7个实际模板 |
| 知识库 | 已完成 | 已完成 | `final-knowledge`；2条目，分类/贡献弹窗另有截图 |
| 统计导出 | 已完成 | 已完成 | `final-statistics`；9文档、2文件、1/4任务、14天趋势 |
| 任务空状态 | 已完成 | 已完成 | `final-dashboard-empty`；真实空团队，创建任务表单另见`final-empty-task-create-narrow.png` |

[事实] 最终浏览器没有新增error，保留ElementPlus分页small弃用warn；构建仍有Sass/大包告警。16:09旧脚本误用取消地址曾产生一次服务ERROR，已修正脚本后33断言通过；没有清空日志掩盖历史错误。运行告警与限制详见日志报告。

[事实] 16:48最终运行复查：前端200、网关UP、21个必要端口监听；11个应用活动日志合计246491字节，历史ERROR共1条，重投异常/内存溢出匹配0条。不是长期零错误保证。证据 `tools/native-runtime/prd-batch11-runtime-final.json`。

## 6. 截图索引

- Batch 0 桌面：`tools/native-runtime/screenshots/prd-round/batch0/dashboard-desktop.png`
- Batch 0 窄屏：`tools/native-runtime/screenshots/prd-round/batch0/dashboard-narrow.png`
- Batch 1 桌面：`tools/native-runtime/screenshots/prd-round/batch1/cancelled-desktop.png`
- Batch 1 窄屏：`tools/native-runtime/screenshots/prd-round/batch1/cancelled-narrow.png`
- Batch 2（同目录 `batch2/`）：`templates-desktop.png`、`templates-narrow.png`、`template-copy-desktop.png`；`knowledge-desktop.png`、`knowledge-narrow.png`、`knowledge-editor-narrow.png`、`knowledge-favorites-desktop.png`、`knowledge-favorites-narrow.png`；`chat-settings-desktop.png`、`chat-search-desktop.png`、`chat-narrow.png`；`documents-desktop.png`、`documents-narrow.png`；`share-desktop.png`、`share-narrow.png`、`export-desktop.png`、`export-narrow.png`。

- Batch 3：`tools/native-runtime/screenshots/prd-round/batch3/` 下 `board-desktop.png`、`board-narrow.png`、`drag-desktop.png`、`project-desktop.png`、`project-narrow.png`、`task-desktop.png`、`task-narrow.png`、`statistics-desktop.png`、`statistics-narrow.png`、`calendar-month-narrow.png`、`calendar-week-narrow.png`、`calendar-week-desktop.png`、`calendar-drag-desktop.png`。
- Batch 4：`tools/native-runtime/screenshots/prd-round/batch4/` 下 `comments-desktop.png`、`comments-narrow.png`、`versions-desktop.png`、`versions-narrow.png`、`diff-desktop.png`、`diff-narrow.png`、`outline-desktop.png`、`outline-narrow.png`、`outline-drag-desktop.png`、`export-options-desktop.png`、`export-options-narrow.png`、`export-success-narrow.png`。
- Batch 5：`tools/native-runtime/screenshots/prd-round/batch5/` 下 `editor-desktop.png`、`editor-narrow.png`、`charts-desktop.png`、`charts-narrow.png`、`groups-narrow.png`、`format-desktop.png`、`format-narrow.png`、`sort-desktop.png`、`merge-desktop.png`、`import-narrow.png`、`export-narrow.png`、`export-success-narrow.png`。

- Batch 6：`tools/native-runtime/screenshots/prd-round/batch6/` 下 `editor-desktop.png`、`editor-narrow.png`、`properties-desktop.png`、`properties-narrow.png`、`collapse-desktop.png`、`org-desktop.png`、`fishbone-desktop.png`、`history-desktop.png`、`presentation-desktop.png`、`presentation-narrow.png`。

- Batch 7：`tools/native-runtime/screenshots/prd-round/batch7/` 下 `search-desktop.png`、`search-narrow.png`、`filters-narrow.png`、`messages-desktop.png`、`message-target-desktop.png`、`commands-desktop.png`、`commands-narrow.png`。

- Batch 8：`tools/native-runtime/screenshots/prd-round/batch8/` 下 `settings-dark-desktop.png`、`settings-narrow.png`、`statistics-desktop.png`、`statistics-narrow.png`、`notifications-desktop.png`、`notifications-narrow.png`。
- Batch 9：`tools/native-runtime/screenshots/prd-round/batch9/`，组织/角色/空间/文档权限/文件夹权限的桌面与窄屏截图；接口证据 `tools/native-runtime/prd-batch9-permissions.json`。
- Batch 10：`tools/native-runtime/screenshots/prd-round/batch10/`，纪要编辑、事项转任务、任务详情的桌面与窄屏截图；接口证据 `tools/native-runtime/prd-batch10-minutes.json`。
- Batch 11 最终全站：`tools/native-runtime/screenshots/prd-round/batch11/final-页面-desktop.png`、`final-页面-narrow.png`；页面英文名逐项见第5节回归表。32张主页面截图、2张空状态、4张知识弹窗和1张任务创建表单，共39张；截图路由/DOM/宽度索引为同目录 `final-regression.json`。
- Batch 11 补充：同目录 `mindmap-drag-desktop.png`、`whiteboard-desktop.png`、`whiteboard-history-desktop.png`、`whiteboard-clear-desktop.png`、`schedule-desktop.png`、`schedule-narrow.png`，以及 `final-knowledge-categories-desktop.png`/`-narrow.png`、`final-knowledge-contributions-desktop.png`/`-narrow.png`。
- 通知补丁：同目录 `notification-dedup-desktop.png`、`notification-dedup-narrow.png`；历史重复通知保留，新事件只生成一条由 `prd-batch11-notification-dedup.json` 证明，不用静态截图代替事件计数。
- 日志检查：`tools/native-runtime/screenshots/runtime-log-check-20261016.png`、`runtime-settings-check-20261016.png`；说明见 `运行日志检查-20261016.md`。截图仅保存在本机Git忽略的运行目录，交付给另一台电脑时需另外复制该证据目录。

## 7. 遗留问题

本节为本机Demo交付边界，不能据此宣传完整PRD或生产就绪；已排除的外部能力不重新引入。

| 状态 | 遗留内容 | 依据与恢复建议 |
| --- | --- | --- |
| 未完成 | Android、OAuth第三方登录、短信、生物识别、音视频与外部设备 | [事实] 用户明确排除；不作为Demo失败项 |
| 未完成 | 离线编辑闭环合并、高级透视表 | [事实] 用户明确本轮不做；恢复时需独立冲突模型/工作簿方案评估 |
| 未完成 | 完整账户找回、在线设备强制下线、多因素/二次验证、完整资料主页 | [事实] 未交付完整Web闭环，现演示入口仅本地登录/注册/偏好；`collab-office-web/src/views/auth/LoginPage.vue:1`、`src/views/settings/PersonalSettingsPage.vue:1`；恢复M1.1/M1.2/M1.6需补页面与服务测试 |
| 部分完成 | 富文本完整工具、行内图片/Word-PDF保真、文档内聊天与访客编辑细节 | [事实] 当前编辑器/导出覆盖主流程，见Batch2/4；恢复M2需单独文件兼容和权限验收 |
| 偏差降级 | 表格完整Excel兼容、大文件、复杂验证/条件格式；导图全主题历史与白板无限画布 | [事实] 已有限额/子集，见Batch5/6/11；升级前保留现存数据兼容 |
| 未完成 | 群头像、消息行内媒体/表情、会话隐藏/清空、桌面通知、文档内聊天 | [事实] 当前聊天入口见`collab-office-web/src/views/im/IMLayout.vue:1`；恢复M5需浏览器实际交互验收 |
| 未完成 | 非音视频会议投票等互动、真实出勤统计 | [事实] 当前计划时长/受邀人数未冒充真实参会；`collab-office-web/src/views/meeting/MeetingListPage.vue:1`；外部邮件/日历服务按用户边界排除 |
| 偏差降级 | 深层子任务、独立项目ACL、完整提醒渠道/日历与冲突规则 | [事实] 仅一层子任务、团队访问和轻量日历提醒；见Batch3 |
| 未完成 | 完整文件版本/复制重命名/分享回收站界面、知识关联图/审批、公开模板市场 | [事实] 云盘和知识库当前入口见`collab-office-web/src/views/file/FileManagePage.vue:1`、`KnowledgeListPage.vue:1`；文档回收站不代表云盘全部具备 |
| 偏差降级 | 搜索/通知分页、白板正文检索、跨浏览器历史；全量翻译/时区、逐篇Word/PDF批量包 | [事实] 有条数/体积和统计窗口限制，见Batch7/8；按需求再扩充 |
| 偏差降级 | 通用RBAC、即时撤销已连接WS及已签下载链接、全局水印/下载限制 | [事实] 五操作权限及已有会话边界，见Batch9；不可直接用于公网/敏感生产数据 |
| 未完成 | 浏览器表格上传最后提交步骤 | [事实] 扩展文件访问开关阻挡；真实multipart接口三格式成功，但不冒充浏览器提交通过，见Batch5 |
| 未完成 | 大规模/长期稳定性和日志强制轮转验证 | [未知] 未做万人连接、100ms达标、长时浸泡、断网/磁盘满测试。132单元测试和本机闭环不能替代这些指标；日志已限额但未人为触发20MB轮转 |

[推断] 当前证据支持本机低负载Demo核心路径可演示；不足以证明所有高级功能、异常组合、长期运行或公网部署均可靠。评论双绑定重复及此前无限重投日志故障已分别修复并验证；队列缺少完整死信人工重放闭环，任意消息重投的业务幂等、可靠补偿仍未作完整保证。

## 8. Batch 12 角色增补验收

2026-10-16按后续用户要求新增系统管理员、项目管理员、普通用户三种平台身份及不同工作台。项目管理员仅管理负责项目；普通用户执行本人任务、提交/回复汇报并在聊天中交流。搜索、统计、导出同步收紧项目范围，后端重新核验身份。V23迁移回填旧项目成员并新建汇报表，所有演示数据只写本机13306、写前备份。

[事实] 本轮39项真实权限断言和角色seed ID集合幂等检查通过；最新各模块测试报告汇总150项通过，包含后续受影响服务单独测试。25张三身份重点页面桌面/窄屏截图保存在batch12。完整账号、权限矩阵、源码及证据路径见 `角色权限与协作验收.md`。

上文“一层子任务、独立项目ACL”遗留中的独立项目成员读权限和单负责人管理已补齐；仍未实现完整多人角色矩阵、不可变创建者审计、即时撤销既有WS等生产RBAC能力。高级功能与性能遗留不因此自动验收通过。

## 2026-10-16 流程复验补充

[事实] 三角色改造后的当前实例复验发现：团队转让给普通用户后管理操作403；系统管理操作统计缺记录链路；内置浏览器PDF预览空白，原因未确认。不能沿用早期批次记录声称当前全部流程闭环。主要Demo链路通过，综合协作34项、角色权限39项通过；150个后端测试经全仓执行与导出产物补跑全部覆盖。完整本轮结果、原始失败、恢复现场与未覆盖范围见 [流程闭环复验](流程闭环复验.md)。
