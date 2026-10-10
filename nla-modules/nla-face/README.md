# nla-face（阶段 4 数据层第一批）

当前交付 `face_person` 的 Person / PersonBo / PersonVo / PersonMapper 与 Mapper XML。模块已登记业务 reactor 和根 POM 坐标管理，尚未接入 `nla-admin`；识别服务、特征缓存、图片上传、鉴权、HTTP 接口与菜单留阶段 6.4。本模块不依赖 `nla-common-facesdk`，不会加载 JNI、模型或创建算法池。

## 新库初始化

建表脚本为 [`script/sql/nla_face.sql`](../../script/sql/nla_face.sql)，在应用的主业务库执行。只声明 `create table if not exists`，无旧数据、无菜单种子、无 drop。已有同名旧表不会自动升级；脚本用于新库初始化，不能代替 schema 升级。此次未执行外部数据库脚本。

| 旧字段/约定 | 新数据层 |
|---|---|
| 自增 `id` | `Long id`、显式 `ASSIGN_ID` 雪花主键，DDL 无自增 |
| `create_user_id` / `update_user_id` | `create_by` / `update_by`，继承 BaseEntity |
| 审计 | 增加 `create_dept`，5 个审计字段由框架填充 |
| Date / 格式注解 | LocalDateTime，无局部格式注解 |
| 物理删除 | `String delFlag` + `@TableLogic`，DDL `char(1)`：0 存在、1 删除 |
| `extract varchar(256)` | `extract longtext`，仍为特征数组字符串，不改变编码格式 |
| 字符集/排序规则 | 显式 `utf8mb4` / `utf8mb4_cs_0900_ai_ci` |

保留图片编号 40、图片地址 256、姓名 40、地址 256 的列长及性别 0/1/2。旧表未约定图片编号唯一性，当前只增加 `(img_id, del_flag)` 普通索引；重复图片编号的业务处理留特征缓存/管理服务迁移时定义。无租户列、无物理外键、无数据导入。

## 查询与转换

- `PersonMapper` 继承 `BaseMapperPlus<Person, PersonVo>`，由应用既有 MapperScan 注册，XML 使用基线 `classpath*:mapper/**/*Mapper.xml` 路径。
- `selectImgIdList(List<String>)` 保留旧方法名，按参数绑定查询有效人员；null、空或不匹配集合返回空列表。XML 显式过滤 `del_flag='0'`，避免原生 SQL 漏掉逻辑删除条件。
- `selectFeatureList()` 是内部特征读取入口，仅选择有效人员的 `id/img_id/extract`，排除空特征；返回部分填充的 Person，不用于人员资料展示。
- 普通 BaseMapper 查询和 `selectImgIdList` 都不选择特征，Person 的 `@TableField(select=false)` 保证这一点。内部写入仍正常保存特征，`toString` 排除特征。
- PersonVo 不包含 extract/delFlag；Bo 也不接受特征、审计或删除字段。使用 MapStruct-Plus 生成 Bo→实体、实体→Vo 转换，不保留手写 Convert 类。
- 新增 Bo 不接受自定义 ID，修改必须提供 Long ID；校验组使用 AddGroup/EditGroup。图片编号/地址/姓名必填，年龄非负、性别 0..2、地址非 null（可为空字符串），长度与 DDL 一致。

`extract` 在库中仍为非空字段，Bo 转实体后必须由后续识别服务生成并写入，再调用 insert。当前没有将 Bo 直接作为完整可保存实体的业务入口。

## 验证

```powershell
mvn -o -B -pl nla-modules/nla-face -am '-Dtest=PersonDataContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

9 项测试实际加载交付 DDL、解析 MyBatis XML 并执行 SQL，覆盖雪花 Long ID、审计时间/用户字段、超过 256 字符的特征保存、普通查询不加载特征、参数绑定与空集合、逻辑删除保留物理行并过滤全部读取入口、更新保留原特征、内部特征读取、生成映射/中文 JSON、分组校验与数据库非空/列长约束。

数据库为 H2 2.4.240 的 MySQL 模式，仅剥离交付 DDL 的 MySQL 表级引擎/字符集/排序规则/行格式选项。测试审计填充器提供固定用户与时间，不依赖登录态。测试不代表真实 MySQL 排序规则、索引性能、生产审计/权限或 JNI 识别验收。
