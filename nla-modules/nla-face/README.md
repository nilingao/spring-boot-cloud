# nla-face

人员信息管理（`face_person` 单表 CRUD）与人脸检测、1:1 比对、1:N 检索，算法底座为 [`nla-common-facesdk`](../../nla-common/nla-common-facesdk/README.md)（SeetaFace JNI 封装，阶段 3.3 交付）。

## 特征来源与门控

- 新增/修改人员时由服务端按 `imgUrl` 下载图片（`FaceImageDownloader`，仅 http/https、限 10 MB、超时兜底），经 `FacePipeline` 提取特征并以 JSON 写入 `extract` 列；识别失败拒绝保存。`imgUrl` 未变化时修改不重算特征（依赖 `updateStrategy=NOT_NULL` 保持原列）。
- 检测流水线移植自旧 `FacePool.Builder`：检测唯一人脸 → 裁剪 → 重检测 → 特征点 → 活体/性别/年龄/双眼/口罩。旧实现右眼状态误取 `detect[0]` 的缺陷已修正为 `detect[1]`。
- 算法代理经 `ObjectProvider` 延迟获取：`face.enabled=false` 时应用与人员 CRUD 正常启动，识别类调用报业务错误"人脸识别功能未启用"。
- `FaceFeatureCache` 替代旧静态 Map：`ApplicationReadyEvent` 全量装载，人员增删改同步维护；图片编号重复时保留先装载的一条并告警（旧 `Collectors.toMap` 重复键直接崩溃）。仅单实例内存语义，多实例各自持有缓存、重启后与数据库对齐。

## 接口与权限

- `/face/person`：list/export/getInfo/add/edit/remove，权限 `face:person:*`，add/edit 走 `AddGroup`/`EditGroup` 校验 + 防重 + 操作日志；`img_id` 在有效人员范围内查重拒绝。
- `/face/detector`：analyze/comparison/search 三个上传端点，定位为基础能力接口，仅 `@SaCheckLogin`，不做按钮级权限。
- 菜单种子见 [`script/sql/nla_face.sql`](../../script/sql/nla_face.sql)（`1761400000000003000` 号段），前端 `face/person/index` 页面由独立前端任务线补齐。

## 验证

```powershell
.migration\test-face-biz.ps1
```

`nla-face` 共 41 项测试（本轮新增 32 项 + 阶段 4.4 的 `PersonDataContractTest` 9 项）全部通过。`FacePipelineTest` 用真实 PNG 解码 + mock 代理验证属性映射（含右眼缺陷修正回归）、人脸数量分支与门控报错；`PersonServiceContractTest` 在 H2 上真实执行 CRUD 写路径（查重拒绝、特征落库、imgUrl 不变不重算、imgId 变更缓存迁移、脏特征回退重提取、删除清缓存）；`FaceDetectorServiceContractTest` 验证检索阈值/topN 与旧默认参数（size=1、calculate=70）；`FaceFeatureCacheTest`、`FaceImageDownloaderTest` 覆盖装载容错与地址白名单。全部测试不触碰 JNI 原生库、不连真实网络。

原生算法联调（`face.enabled=true` + dll/csta 配置）、真实 MySQL 菜单脚本执行与前端页面仍需对应环境验收。迁移状态以 [`docs/migration/mapping.md`](../../docs/migration/mapping.md) 为准。
