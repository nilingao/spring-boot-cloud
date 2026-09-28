---
trigger: model_decision
description: 逆龙傲 spring-boot-cloud 项目概况：技术栈与框架版本（JDK 1.8、Spring Boot 2.3.2、Spring Cloud Hoxton.SR9、Alibaba 2.2.6、Nacos、MyBatis-Plus、Easy-ES、Redis、RabbitMQ、MinIO、OAuth2、Sentinel、Seata、SkyWalking、XXL-JOB、MapStruct、Hutool、Activiti、FreeSWITCH、SeetaFace6、Docker）；7 个一级模块与约 45 个子模块的职责划分与单向依赖流向；运行环境（数据源、Nacos shared-configs、构建产物、基础镜像、镜像仓库、JVM 参数、端口以 .run/*.run.xml 为准）；9 大业务领域定位。加载场景：询问项目是什么、用了哪些技术、框架版本、模块划分、目录结构、服务端口、部署方式、业务功能范围。
---

# 逆龙傲 spring-boot-cloud 项目概况（技术栈 / 模块 / 环境 / 业务定位）

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

## 〇、项目概况

### 0.1 技术栈

**类型**：Spring Cloud Alibaba 微服务中后台脚手架（多租户、多业务域）。

| 维度 | 结论 | 实证 |
|---|---|---|
| 语言/JDK | Java **1.8** | 根 `pom.xml` `<java.version>`；IDE SDK `Oracle OpenJDK 1.8.0_341` |
| 框架 | Spring Boot **2.3.2.RELEASE** | 根 POM `<parent>` |
| 微服务 | Spring Cloud **Hoxton.SR9** + Alibaba **2.2.6.RELEASE** | 根 POM `<properties>` |
| 构建 | Maven 多模块（7 个一级，约 45 个子模块） | 根 POM `<modules>` |
| 注册/配置 | Nacos **2.1.0**（shared-configs 8 份） | 根 POM + `bootstrap.yml` |
| 数据库 | MySQL **5.7**，每模块独立库 | `.idea/dataSources.xml`、`docker-compose-server.yml`、`sql/*.sql` |
| ORM | MyBatis-Plus **3.4.3** + dynamic-datasource **3.2.0** + sharding-jdbc **4.1.0** | 根 POM |
| 搜索 | Elasticsearch **7.14.0** + Easy-ES **1.1.1** | 根 POM、`@EsMapperScan` |
| 缓存 | Redis（`RedisUtils`/`RedisService`） | `starter-redis` |
| 消息 | RabbitMQ（`RabbitAdmin` + `ContentTypeDelegatingMessageConverter`） | `MQConfig.java` |
| 对象存储 | MinIO **8.0.3** | 根 POM、`MinioTest.java` |
| 认证 | Spring Security + OAuth2 + JWT | `starter-security-oauth-{basic,core}` |
| 熔断限流 | Sentinel **1.8.3** | `AbstractSentinelExceptionHandler` |
| 分布式事务 | Seata **1.5.0**（AT，`@GlobalTransactional`） | `service-seata`、web-api Service |
| 链路追踪 | SkyWalking **9.1.0**（Agent + gRPC 日志上报） | `start/logback.xml`、根 POM |
| 定时任务 | XXL-JOB **2.3.1**（quartz 已弃用） | `README.md` L90 |
| 对象映射 | MapStruct **1.4.1.Final** + Lombok **1.18.20** | 根 POM `annotationProcessorPaths` |
| 工具库 | Hutool **5.5.8** | 根 POM |
| 工作流 | Activiti **7.1.0.M6** | `spring-boot-activiti` |
| 通信 | netty-socketio、FreeSWITCH ESL、GB/T 28181 | starter 目录、`spring-boot-video` |
| 人脸 | SeetaFace6 | `spring-boot-face` |
| API 文档 | Swagger | `starter-swagger` |
| 日志 | Log4j2 API + Logback 实现 | `@Log4j2`、`start/logback.xml` |
| 部署 | Docker + compose + Jenkins + 腾讯云镜像仓库 | `Dockerfile`、`docker-image-build.sh` |

### 0.2 模块划分与依赖流向

| 一级模块 | 职责 | 子模块 |
|---|---|---|
| `spring-boot-common` | 共享基础 | `comm`（工具/响应/异常/常量/枚举）、`entity`（Entity/Param/Vo/ES/MQ） |
| `spring-boot-starter` | 技术封装（21） | `autopoi`、`cloud`、`elasticsearch`、`feign`、`freeswitch`、`logs`、`minio`、`mybatis`、`nacos`、`netty`、`pay`(8 渠道)、`quartz`、`rabbitmq`、`redis`、`security-oauth`、`sentinel`、`sms`、`socket-io`、`swagger`、`video`、`xxl-job` |
| `spring-boot-feign` | 调用契约 | `feign-{activiti,bean,face,oa,sms,sso,video}` |
| `spring-boot-business` | 业务服务（7） | `activiti`、`bean`、`face`、`fs`、`oa`、`sms`、`video` |
| `spring-boot-client` | 客户端聚合（2） | `web-api`(`/webapi/**`)、`app`(`/app/**`) |
| `spring-boot-system` | 系统服务（3） | `gateway`、`sso`、`pay` |
| `spring-boot-service` | 本地起中间件（4，**禁写业务**） | `nacos`、`seata`、`sentinel`、`xxl-job` |

依赖单向：`client → feign → business → starter → common`；`common` 不依赖上层；**业务模块间禁止直接依赖**，只能经 feign 契约。

### 0.3 运行环境

| 项 | 值 | 实证 |
|---|---|---|
| JDK | `D:/develop/Jdk/jdk1.8.0_341` | IDE Context |
| Git / 分支 | `https://github.com/NiLongAo/spring-boot-cloud.git` / `5.0.0-dev` | IDE Context |
| 主数据源 | `jdbc:mysql://1.82.217.118:3401/sys_bean_dev`（root） | `.idea/dataSources.xml` |
| 中间件地址 | `1.82.217.118`；SkyWalking OAP gRPC `:12011` | `bootstrap-dev.yml`、`start/logback.xml`、compose |
| 环境切换 | `spring.profiles.active`（本地 `dev`，Docker `prod`） | `bootstrap.yml`、`Dockerfile` |
| Nacos shared-configs | `common-config`、`minio-config`、`ribbon-config`、`sentinel-config`、`mybatis-config`、`seate-config`（原文拼写）、`redis-config`、`nacos-config`，均带 `-{env}.yaml` | `bootstrap.yml` |
| 构建产物 | `spring-boot-maven-plugin layout=ZIP` + `<includes>non-exists</includes>`，依赖外置 `target/lib`；SkyWalking agent → `target/tmp` | 根 POM、`spring-boot-fs/pom.xml` |
| 全局测试开关 | `maven-surefire-plugin <skipTests>true</skipTests>` | 根 POM |
| 编码 | JVM `-Dfile.encoding=UTF-8` + resources 插件 UTF-8 | 根 POM |
| 基础镜像 | `anapsix/alpine-java:8_server-jre_unlimited` | `Dockerfile` |
| 镜像仓库 | `ccr.ccs.tencentyun.com/spring_boot_cloud/spring_cloud_{module}:latest` | `docker-image-build.sh` |
| 日志卷 | 宿主 `/work/spring-cloud/spring-boot-{module}/logs` | `.run/*.run.xml` |
| **端口/调试端口/容器名** | **以 `.run/{module}.run.xml` 为单一事实来源**（`portBindings`/`volumeBindings`/`imageTag`/`containerName`/`envVars`，共 11 个模块），本文不复述以防漂移 | `.run/` |
| Docker JVM 参数 | `-javaagent:skywalking-agent.jar` + `-Dskywalking.agent.service_name` + `-Dskywalking.collector.backend_service` + `agentlib:jdwp` + `MetaspaceSize=256M` + `Xms/Xmx=256m -Xss512k` + `UseG1GC G1HeapRegionSize=4M` + `-Dloader.path=lib` + `-Djava.security.egd=file:/dev/./urandom` + `--logging.config=start/logback.xml` | `spring-boot-bean/Dockerfile` |

### 0.4 业务定位（9 领域）

1. **统一认证与权限**（`sso`+`gateway`）：OAuth2+JWT，多登录方式（密码/短信/微信小程序），网关 URL-角色集中鉴权，多租户
2. **组织与用户主数据**（`bean`）：部门树、用户、角色、权限、字典、区域
3. **国标视频监控**（`video`+`starter-video`）：GB/T 28181 设备接入、通道、录像、云台
4. **呼叫中心/融合通信**（`fs`+`starter-freeswitch`）：平台、媒体服务器、网关、路由、企业资源、技能组、坐席、IVR、VDN、通话记录（**41 张表**，页面归属见 `gaps-and-fs-tables.md` 附录）
5. **工作流**（`activiti`）：Activiti 7 流程定义与实例
6. **人脸识别**（`face`）：SeetaFace6
7. **短信与实时推送**（`sms`+`starter-sms`+`starter-socket-io`）：多渠道短信、RabbitMQ、Socket.IO
8. **支付**（`pay`+`starter-pay` 8 渠道）：⚠️ **暂未开发**（README 标注）→ 金额约定缺失，见 `gaps-and-fs-tables.md`
9. **OA**（`oa`）：办公自动化

