Collab Office 完全离线安装包
================================

目标电脑要求：
1. Windows 10/11 64 位。
2. 已安装 Docker Desktop，并使用 Linux Containers。
3. Docker Desktop 至少分配 8 GB 内存。
4. 目标电脑不需要 Java、Maven、Node.js、MySQL 或网络连接。

安装：
1. 将整个离线安装包文件夹复制到目标电脑本地磁盘。
2. 双击 docker-offline-import.bat。
3. 等待镜像校验、导入和服务初始化完成。
4. 浏览器会自动打开 http://localhost:3030。

停止：
双击 docker-offline-stop.bat。停止操作会保留所有业务数据。

重新启动：
再次双击 docker-offline-import.bat 即可；已有 .env 和数据卷会保留。

注意：
- 不要只复制 collab-office-images.tar，迁移脚本和 Compose 文件也是运行必需的。
- collab-office-images.tar 是一个归档文件，内部包含本系统所需的全部镜像。
- 不要执行 docker compose down -v，除非确定要清空所有业务数据。
- 离线镜像包与目标 Docker 的 CPU 架构最好一致，例如 linux/x86_64。
