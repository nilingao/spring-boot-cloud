# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds
# FROM bellsoft/liberica-openjdk-rocky:25.0.4-cds
# FROM findepi/graalvm:java21-native

LABEL maintainer="TZY"

RUN mkdir -p /nla/server/logs \
    /nla/server/temp \
    /nla/skywalking/agent

WORKDIR /nla/server

ENV SERVER_PORT=8080 SNAIL_JOB_PORT=28080 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}
# 暴露 snail 客户端端口 用于定时任务调度中心通信
EXPOSE ${SNAIL_JOB_PORT}

ADD ./target/nla-admin.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           -Dsnail-job.port=${SNAIL_JOB_PORT} \
           # 应用名称 如果想区分集群节点监控 改成不同的名称即可
           #-Dskywalking.agent.service_name=nla-server \
           #-javaagent:/nla/skywalking/agent/skywalking-agent.jar \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
