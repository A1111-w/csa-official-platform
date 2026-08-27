# 备份、恢复与演练

## 备份内容

`deploy/backup.ps1` 从 Compose 服务内部执行：

- MySQL：`mysqldump --single-transaction --quick --routines --events --triggers --hex-blob`，输出 `database.sql.gz`。
- 上传文件：从 `/app/uploads` 生成 `uploads.tar.gz`。
- 元数据：`metadata.json`，不含密码、Token 或 Cookie。
- 完整性：`SHA256SUMS` 覆盖两个归档和元数据。

MySQL 与文件快照不是跨系统原子快照。备份窗口内应停止高频写操作；恢复时脚本会停止 `caddy/frontend/backend`，避免应用继续写入。

## 执行

生产机上使用外部秘密文件或环境管理器，不把值写入命令历史：

```powershell
.\deploy\backup.ps1 `
  -ComposeFile .\compose.production.yml `
  -EnvFile C:\secure\csa-production.env `
  -ProjectName csa-production `
  -OutputRoot D:\backup\csa `
  -RetentionDays 30
```

保留策略至少应满足：每日备份保留 30 天、每周备份保留 12 周、每月备份保留 12 个月。脚本负责每日目录的 `RetentionDays` 清理，周/月副本应由外部备份系统复制后管理。

## 恢复

恢复是破坏性操作，必须先拿当前库做一次新备份，并在维护窗口执行：

```powershell
.\deploy\restore.ps1 `
  -BackupDirectory D:\backup\csa\20260730T010000Z `
  -ComposeFile .\compose.production.yml `
  -EnvFile C:\secure\csa-production.env `
  -ProjectName csa-production `
  -ConfirmDestructiveRestore
```

脚本会拒绝缺少 checksum 或 checksum 不匹配的目录，并重建目标数据库后导入归档。上传卷会先清空再解包；不需要恢复文件时加 `-SkipUploads`。失败时应用服务保持停止，先从最后一个已验证备份处理，不要直接重启接受半恢复状态。

## 加密与存储

备份离开部署主机前必须加密。推荐使用云 KMS 管理的 envelope key，或使用 `age` 公钥加密后再上传对象存储；私钥不放在服务器、仓库和同一备份目录。传输使用 HTTPS/SFTP，存储桶开启服务端加密、版本控制和不可变保留策略。SHA-256 只证明完整性，不提供保密性。

## 恢复演练清单

每季度在隔离项目执行一次：

1. 生成备份并保存 checksum。
2. 在空 MySQL/Redis/上传卷中恢复。
3. 查询关键表行数、登录测试账号、读取一个上传文件，并检查 Flyway history。
4. 启动 backend/frontend，验证 liveness、readiness、登录、CSRF、权限和文件下载。
5. 删除演练资源，记录 RTO、RPO、耗时、失败点和修正负责人。

演练中使用专门的测试账号和临时秘密，不能把 `db/seed.sql` 中的演示密码当成生产恢复凭据。

2026-07-29 的隔离演练已验证数据库值和上传文件从修改后的状态恢复到备份状态，SHA-256 校验一致，应用 readiness 恢复为 UP。演练没有使用生产账号、生产秘密或真实邮件凭据。2026-07-30 当前源码的复演因 Docker 数据盘 I/O 故障暂停，恢复 Docker/staging 后必须再跑一次并更新阶段验证记录。
