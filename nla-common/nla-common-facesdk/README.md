# nla-common-facesdk

SeetaFace 6 的 JDK 21 / Spring Boot 4 技术封装。保留 29 个 `com.seeta.sdk` 源文件的 JNI 包名、
native 方法签名和字段，提供 16 组对象池/代理及自动配置。业务表和 Controller 在阶段 6.4 接入。

消费方通过 `nla-common-bom` 管理依赖：

```xml
<dependency>
    <groupId>cn.com.nla</groupId>
    <artifactId>nla-common-facesdk</artifactId>
</dependency>
```

默认关闭，应用启动不会加载 DLL/SO。DLL、SO、`.csta` 模型与各平台目录下的 `dll.properties` 清单
**都不打入模块 JAR、也不纳入版本管理**（单文件接近 100 MiB，入仓会使仓库膨胀且逼近 GitHub 上限），
由部署环境单独提供。本仓库开发机的约定是把整个外置包放在 `.lib-service/` 下（`.gitignore` 已忽略，
与 `allatori` / `rabbitmq-plugins` / `skywalking-agent` 同一惯例）。启用配置：

```yaml
face:
  enabled: true
  # 开发机：仓库内 .lib-service/（已 gitignore）；生产：拷到部署目录（相对路径按 JVM 工作目录解析）
  dll-path: F:/work/java/spring-boot-cloud/.lib-service/seetaface6
  csta-path: F:/work/java/spring-boot-cloud/.lib-service/sf3.0_models
  device: SEETA_DEVICE_CPU # AUTO 默认选择 CPU 库，GPU 需匹配的 amd64 库
  device-id: 0
  max-total: 8 # 每种算法独立对象池
  max-idle: 8
  min-idle: 0
  max-wait: 30s
```

沿用旧 `face.dll-path` / `face.csta-path`。`dll-path` 可以指向原生包根目录或含 `dll.properties` 的
平台目录；根目录布局为 `windows/amd64` 或 `linux/{amd64,aarch64,arm}`。按 manifest 数字顺序
加载 `base` 库、再加载 JNI 库；`tennis` 从 `base/CPU` 或 `base/GPU` 读取。

外置包实测覆盖 Windows amd64（25 个 DLL）与 Linux amd64 / aarch64 / arm 三套（73 个 SO），
GPU 版 `tennis` 位于 `windows/amd64/base/GPU` 与 `linux/amd64/base/GPU`；14 个 `.csta` 模型与
4 份 `dll.properties` 随包提供（合计 116 个文件 / 426,004,284 字节）。
⚠️ Linux **arm（32 位）** 那份 `dll.properties` 用的 key 前缀是 `linux_centos.*`，而
`LoadNativeCore.resolveLibraries` 只识别 `so.base.N` / `so.N`，否则抛
`Unknown native manifest entry` —— 部署到 32 位 ARM 前需把该份 manifest 的前缀改写为 `so.`
（64 位 ARM 用 `aarch64` 那套，无需改写）。

自动配置启动前检查库清单、全部库文件与默认算法使用的 10 个模型文件；缺文件、加载失败或对象池
参数错误时启动失败，异常包含问题路径。同一 JVM 中成功加载后不能切换库目录或计算设备。
默认 `QualityOfLBN` 使用 `quality_lbn.csta`，已修正旧配置指向 `pose_estimation.csta` 的错误。

用法示例：

```java
SeetaImageData image = SeetafaceUtil.toSeetaImageData(inputStream);
SeetaRect[] faces = faceDetectorProxy.detect(image);
```

代理保留原算法方法名，算法或对象池异常会向调用方抛出 `IllegalStateException`。
代理中的对象池在应用关闭时释放 JNI 句柄；手工创建的 SDK 对象支持 `AutoCloseable`，应使用
try-with-resources 或 `close()`，避免只依赖 `finalize()`。保留 native `dispose()` 以维持 JNI ABI，
新代码统一使用幂等的 `close()`。

`SeetafaceUtil` 不依赖 Spring Web：消费方将上传文件的流传入 `toSeetaImageData(InputStream)`；
输出通过 `toImageBytes(data, "png")` 等编码成图像文件。流由调用方关闭。旧 `toMultipartData`
方法由业务 Controller 适配，不能把原始 BGR 像素当 PNG/JPEG 文件。编码与解码保留 BGR 通道顺序。

契约测试无需原生库：

```text
mvn -o -B -pl nla-common/nla-common-facesdk -am -Dtest=FaceSdkContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

真实 JNI 冒烟需显式传入外部路径：

```text
mvn -o -B -pl nla-common/nla-common-facesdk -am -Dtest=FaceSdkContractTest,FaceSdkNativeSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dface.native.path=F:/work/java/spring-boot-cloud/.lib-service/seetaface6 -Dface.model.path=F:/work/java/spring-boot-cloud/.lib-service/sf3.0_models test
```

已在 Windows amd64 / JDK 21 / CPU 验证 16 种算法句柄创建与释放、检测器收图、特征维度查询、
重复关闭和图像编码往返。Linux/GPU、真实人脸识别准确率、活体识别效果及生产负载留待对应环境验证。
