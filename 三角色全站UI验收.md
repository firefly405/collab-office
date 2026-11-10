# 三角色全站UI验收

日期：2026-10-16，batch15。打开 <http://127.0.0.1:3000/#/im>，沿用现有三种演示账号。

## 修复结果

[事实] 原先角色类名还要求路由为首页，切到消息、文档等模块就退出了角色样式。现改为只按登录身份确定 `role-member`、`role-project`、`role-admin`，首页内容与聊天布局另用页面类隔离。证据：`collab-office-web/src/layouts/MainLayout.vue:2`。因此这次是全站外框更新，替代前轮“只在首页生效”的设计。

| 身份 | 全站导航与页面形态 | 代码证据 |
| --- | --- | --- |
| 普通用户 | 左侧悬浮窄导航、独立圆角边界、浅色画布、绿色主要操作；顶部团队选择保留。聊天为绿色气泡和圆形会话头像 | [事实] `collab-office-web/src/layouts/MainLayout.vue:114`、`:156` |
| 项目管理员 | 导航移至顶部两行；品牌、团队选择与团队/设置入口在首行，业务模块横向排列。窄屏可横向滚动，所有入口保留 | [事实] `collab-office-web/src/layouts/MainLayout.vue:128`、`:169` |
| 系统管理员 | 各页面保持深色左侧导航、紧凑顶栏、管理分组及当前模块提示。聊天使用直角边界和灰色选中态 | [事实] `collab-office-web/src/layouts/MainLayout.vue:143`、`:160` |

[事实] 只修改共享布局文件，没有修改业务接口、后端权限、登录流程、聊天逻辑、数据库或演示seed。模块标题由现有导航匹配，主要操作颜色通过局部Element Plus变量统一；`collab-office-web/src/layouts/MainLayout.vue:76`、`:113`。

[事实] 聊天区改按可用空间伸缩，避免角色顶栏高度不同导致发送区跑到屏幕外；`collab-office-web/src/layouts/MainLayout.vue:106`、`:154`。消息、任务等模块内部操作仍使用原组件。

## 本轮验证

| 检查 | 状态 | 结果与证据 |
| --- | --- | --- |
| 前端构建及TS检查 | 已完成 | `npm --prefix collab-office-web run build`退出0，含vue-tsc，最终Vite构建8.11秒；`tools/native-runtime/role-shell-build.log` |
| 三身份跨页面外框 | 已完成 | 普通用户14页、PM14页、管理员15页，每页桌面1440和窄屏390各检查一次，共86次；`shell-checks.json`记录每次角色类名与页面/内容尺寸 |
| 页面级横向溢出 | 已完成 | 上述86次检查中，根宽等于根滚动宽，内容宽等于内容滚动宽；表格自身及PM导航的内部滚动保留 |
| 首页进入消息 | 已完成 | 三身份均经实际导航进入同一演示群，角色外框保持一致，内容和原有操作可见；三组message桌面/窄屏截图 |
| 消息页刷新 | 已完成 | 普通用户刷新后仍为role-member；管理员刷新后仍为role-admin，导航背景rgb(25,29,35)。重新选择会话可正常加载 |
| 聊天输入区 | 已完成 | 三身份390宽时发送区底部约831px，小于视口844px；输入框及发送按钮完整可见，消息列表内部滚动 |
| 后端测试、业务深度回归 | 未完成 | 本轮未改后端，未重跑后端测试；未重新测试导出、协同冲突、角色变更或压力指标 |

86次检查覆盖：工作台、云文档、任务、云盘、会议、表格列表、导图列表、白板列表、汇报、模板、知识库、统计、设置、团队成员；管理员另含系统管理。消息另做三身份桌面/窄屏实际会话目检。上述检查是导航、外框、尺寸与截图检查，不是所有功能的验收。

## 截图索引

目录：`tools/native-runtime/screenshots/prd-round/batch15/`。截图及JSON属于本机运行证据，按仓库规则不提交runtime目录。

| 内容 | 文件 |
| --- | --- |
| 同一消息页三角色对比 | three-role-message-comparison.png（实际截图缩放拼接） |
| 普通用户消息 | member-message-desktop.png、member-message-narrow.png |
| 项目管理员消息 | manager-message-desktop.png、manager-message-narrow.png |
| 管理员消息 | admin-message-desktop.png、admin-message-narrow.png |
| 各模块 | member/manager/admin-模块-desktop/narrow.png；模块名见shell-checks.json的page字段 |
| 外框与尺寸记录 | shell-checks.json，共86条 |

## 边界与遗留

- [事实] 登录、注册、公开分享页不经过MainLayout，不按尚未登录的身份套角色外框；`collab-office-web/src/router/index.ts:5`。公开分享权限未改动。
- [事实] 未重新操作表格、思维导图、白板的具体编辑器画布，列表页的检查不能代替画布及其高级功能回归。
- [事实] 本轮未发送新聊天内容，未改项目或角色；打开会话会沿用原逻辑标记已读。切换账号时曾出现“登录已过期”提示，重新登录成功，本轮未修改鉴权流程。
- [事实] 原有大包体告警仍在，没有提高阈值掩盖告警。未验证暗色主题、其他浏览器、极端缩放和长期运行。

服务保持运行，浏览器留在普通用户消息页。此前首页截图属于历史版本，最新跨页面效果以本报告为准。
