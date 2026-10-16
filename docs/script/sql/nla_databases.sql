-- ----------------------------
-- NLA Cloud 分库建库脚本（阶段 9 部署交付物）
--
-- 本工程采用分库模式，共 8 个库：主库 nla_system + 4 个业务分库 + 工作流 / 调度 / AI 库。
-- 应用侧的路由方式是 dynamic-datasource 多数据源 + 业务 Mapper 接口上的 @DS("库名")，
-- 决策依据与落地清单见 docs/migration/mapping.md 7.9。
--
-- 使用方式（任选其一）：
--   1) 手工执行：mysql -h <host> -P <port> -u root -p < nla_databases.sql
--   2) Docker：script/docker/docker-compose.yml 已把 /docker/mysql/init/ 挂载到容器内的
--      /docker-entrypoint-initdb.d/，将本脚本放入该目录即可在 MySQL 首次初始化时自动执行。
--      注意：官方镜像只在数据目录为空时执行 init 脚本，已有数据卷不会重跑。
--
-- 本脚本只建库，不建表、不导菜单种子。表结构与种子按库名归位手工执行：
--   nla_system     <- nla_system.sql + nla_system_ext.sql + nla_system_area_data.sql
--                    + 各业务模块 SQL 里的菜单/授权种子段
--   nla_message    <- nla_message.sql
--   nla_face       <- nla_face.sql
--   nla_video      <- nla_video.sql
--   nla_callcenter <- nla_callcenter.sql
--   nla_workflow   <- nla_workflow.sql
--   nla_job        <- nla_job.sql
--   nla_ai         <- nla_ai.sql
--
-- 排序规则口径见 docs/migration/mapping.md 7.7：全库统一 utf8mb4 + utf8mb4_cs_0900_ai_ci。
-- nla_system / nla_job / nla_workflow 的建表语句本身不写 COLLATE，靠这里的库默认值继承，
-- 因此建库时必须显式指定，不能省。
-- ----------------------------

CREATE DATABASE IF NOT EXISTS `nla_system`     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_message`    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_face`       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_video`      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_callcenter` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_workflow`   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_job`        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `nla_ai`         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_cs_0900_ai_ci;
