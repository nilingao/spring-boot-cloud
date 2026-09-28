---
trigger: model_decision
description: 可直接照抄的分层标准骨架：业务模块完整目录树、启动类标准注解组合、共享模块（comm / entity）结构、ServiceImpl 标准骨架（增删改查、引用校验、@Transactional、缓存事件）、Controller 标准骨架（extends ApiController、RestResult 返回、@Validated 分组校验、@ApiLog）、Mapper XML 编写规范、MapStruct Convert 转换器写法（INSTANCE）、Feign 契约接口定义与调用方式。加载场景：新建模块或一套完整的 CRUD / 管理端接口、写 Mapper XML、定义 Feign 接口、需要参考标准代码模板与骨架。
---

# 分层架构与标准代码骨架模板（Service / Controller / Mapper XML / MapStruct / Feign）

> 规范分册之一（原 `project-conventions.md` 按章拆分而来，结论均基于 `5.0.0-dev` 分支代码实证）。常驻铁律与全部分册索引见 `.qoder/rules/project-baseline.md`；查不到的机制见 `gaps-and-fs-tables.md`，禁止编造。

## 四、分层架构与标准开发模式

### 4.1 业务模块目录

```
spring-boot-business/spring-boot-{module}/
├─ src/main/java/cn/com/tzy/springboot{module}/
│  ├─ SpringBoot{Module}Application.java
│  ├─ controller/api/{group}/
│  ├─ service/api/{Xxx}Service.java + service/api/impl/
│  ├─ service/manage/{group}/ + impl/          （新规范）
│  ├─ mapper/sql/{Xxx}Mapper.java、mapper/es/
│  ├─ convert/{group}/{Xxx}Convert.java
│  ├─ config/、exception/、utils/
├─ src/main/resources/
│  ├─ bootstrap.yml、bootstrap-dev.yml、bootstrap-prod.yml
│  ├─ logback-test.xml
│  └─ mapper/{Xxx}Mapper.xml
├─ src/test/java/cn/com/tzy/springboot{module}/
├─ start/{logback.xml, logback-mq.xml, start.sh}
└─ Dockerfile、docker-image-build.sh、pom.xml
```

注：`video` 模块 Service 直接在 `service/` + `service/impl/`（无 `api` 中间层），属历史差异。

### 4.2 启动类标准注解

```java
@EnableDiscoveryClient                                              // Nacos 注册发现
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // 数据源由 sharding/dynamic 接管
@RibbonClients(defaultConfiguration = MyRibbonConfig.class)          // Nacos 权重负载
@EsMapperScan("cn.com.tzy.springboot{module}.mapper.es")             // 用 ES 时
@EnableCaching                                                       // Spring Cache
public class SpringBoot{Module}Application {
    public static void main(String[] args) { SpringApplication.run(...); }
}
```

### 4.3 共享模块结构

`spring-boot-entity`：`dome/{bean,fs,video,sms,oa,face,sys}`、`param/{bean,fs,video,sms,oa,sys,activiti}`、
`vo/{bean,fs,video}`、`es/`、`export/`、`mq/`、`common/`、`utils/`（`TreeUtil`）

`spring-boot-comm`：`common/bean`（`Base`、`LongIdEntity`、`IntIdEntity`、`StringIdEntity`、`TreeNode`）、
`common/model`（`BaseModel`、`PageModel`、`PageSortModel`、`DictModel`）、
`common/vo`（`RestResult`、`PageResult`、`RespCode`）、`common/enumcom`（`ConstEnum`）、
`common/jwt`（`JwtCommon`）、`common/mq`、`constant`（`Constant`、`NotNullMap`）、
`excption`（6 个异常类）、`interfaces`、`spring`（`DateEditor`）、`utils`（`AppUtils`、`JwtUtils`）

### 4.4 Service 实现标准骨架

```java
@Service
public class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService {

    @SneakyThrows
    @Override
    public PageResult page(XxxParam param) {
        Page<Xxx> page = MyBatisUtils.buildPage(param);
        LambdaQueryWrapper<Xxx> w = new LambdaQueryWrapper<Xxx>()
                .and(StringUtils.isNotBlank(param.getQuery()), q -> q.like(Xxx::getName, param.getQuery()))
                .eq(param.getStatus() != null, Xxx::getStatus, param.getStatus());
        return MyBatisUtils.selectPage(baseMapper.selectPage(page, w));
    }

    @Override
    public RestResult<XxxDetailVo> detail(Long id) {
        Xxx e = getById(id);
        if (e == null) return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        return RestResult.result(RespCode.CODE_0.getValue(), null, XxxConvert.INSTANCE.convert(e));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestResult<Long> save(XxxSaveParam param) {
        boolean isUpdate = param.getId() != null;
        if (isUpdate && getById(param.getId()) == null)
            return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        long cnt = count(new LambdaQueryWrapper<Xxx>().eq(Xxx::getName, param.getName())
                        .ne(isUpdate, Xxx::getId, param.getId()));     // 唯一性预校验，编辑排除自身
        if (cnt > 0) return RestResult.result(RespCode.CODE_2.getValue(), "名称已存在");
        Xxx entity = XxxConvert.INSTANCE.convert(param);
        if (!(isUpdate ? updateById(entity) : save(entity)))
            return RestResult.result(RespCode.CODE_2.getValue(), "保存失败");
        return RestResult.result(RespCode.CODE_0.getValue(), null, entity.getId());
    }

    @Override
    public RestResult<?> status(FsLongStatusParam param) {
        if (getById(param.getId()) == null)
            return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        Xxx update = new Xxx();                 // 只更新目标字段
        update.setId(param.getId());
        update.setEnable(param.getStatus());
        if (!updateById(update)) return RestResult.result(RespCode.CODE_2.getValue(), "状态更新失败");
        return RestResult.result(RespCode.CODE_0.getValue(), "更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestResult<?> remove(Long id) {
        Xxx e = getById(id);
        if (e == null) return RestResult.result(RespCode.CODE_2.getValue(), "xxx不存在");
        if (refService.count(new LambdaQueryWrapper<Ref>().eq(Ref::getXxxId, id)) > 0)
            return RestResult.result(RespCode.CODE_2.getValue(), "请先删除子级xxx");
        removeById(id);
        return RestResult.result(RespCode.CODE_0.getValue(), "删除成功");
    }

    @Override
    public RestResult<List<XxxOptionVo>> select(String keyword, Integer limit) {
        int n = limit != null ? limit : 20;
        List<Xxx> list = list(new LambdaQueryWrapper<Xxx>()
                .like(StringUtils.isNotBlank(keyword), Xxx::getName, keyword)
                .last("LIMIT " + n));
        return RestResult.result(RespCode.CODE_0.getValue(), null, XxxConvert.INSTANCE.convertOptions(list));
    }
}
```

管理 Service（fs 新规范）**不继承 `ServiceImpl`**，改为注入基础 Service + Mapper：

```java
@Service
public class PlatformManageServiceImpl implements PlatformManageService {
    private final PlatformService platformService;
    private final PlatformMapper platformMapper;
    public PlatformManageServiceImpl(PlatformService s, PlatformMapper m) {
        this.platformService = s; this.platformMapper = m;
    }
}
```

### 4.5 Controller 标准骨架

```java
@RestController("Api{Module}{Resource}Controller")
@RequestMapping("/api/{module}/{resource}")
public class XxxController extends ApiController {          // 必须继承

    private final XxxManageService xxxManageService;          // 新规范：构造器注入
    public XxxController(XxxManageService s) { this.xxxManageService = s; }

    @PostMapping("page")
    public PageResult page(@Validated @RequestBody XxxPageParam param) { return xxxManageService.page(param); }

    @GetMapping("detail")
    public RestResult<XxxDetailVo> detail(@RequestParam Long id) { return xxxManageService.detail(id); }

    @PostMapping("save")
    public RestResult<Long> save(@Validated @RequestBody XxxSaveParam param) { return xxxManageService.save(param); }

    @PostMapping("status")
    public RestResult<?> status(@Validated @RequestBody FsLongStatusParam param) { return xxxManageService.status(param); }

    @DeleteMapping("remove")
    public RestResult<?> remove(@RequestParam Long id) { return xxxManageService.remove(id); }

    @GetMapping("select")
    public RestResult<List<XxxOptionVo>> select(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "20") Integer limit) {
        return xxxManageService.select(keyword, limit);
    }
}
```

旧模块额外带冗余 `@ResponseBody`；client 层必写 Swagger 注解：

```java
@Api(tags = "部门信息相关接口", position = 1)
@ApiOperation(value = "部门信息分页查询", notes = "部门信息分页查询")
@ApiImplicitParams({
    @ApiImplicitParam(name = "id", value = "部门信息编号", required = true,
                      paramType = "query", dataType = "Long", example = "0")
})
```

### 4.6 Mapper XML 规范

```xml
<mapper namespace="cn.com.tzy.springboot{module}.mapper.sql.XxxMapper">
  <resultMap id="BaseResultMap" type="cn.com.tzy.springbootentity.dome.{module}.Xxx">
    <!--@mbg.generated-->
    <!--@Table bean_department-->
    <id column="id" property="id"/>
    <result column="parent_id" property="parentId"/>
  </resultMap>

  <sql id="Base_Column_List">
    <!--@mbg.generated-->
    id, parent_id, department_name
  </sql>

  <select id="findAvailableTree" resultType="map">
    select parent_id as 'parentId', id as 'id', department_name as 'departmentName'
    from bean_department
    <trim prefix="where" prefixOverrides="and">
      and is_enable = 1
      <if test="departmentName != null and departmentName != ''">
        and department_name like concat('%',#{departmentName},'%')
      </if>
    </trim>
  </select>

  <select id="selectNameLimit" resultType="cn.com.tzy.springbootentity.dome.bean.Department">
    select bu.* from bean_department bu
    <trim prefix="where" prefixOverrides="and">
      <if test="departmentIdList != null and departmentIdList.size() != 0">
        <foreach close=")" collection="departmentIdList" item="item" open="and bu.id not in(" separator=",">
          #{item}
        </foreach>
      </if>
    </trim>
    <if test="limit != null"> limit ${limit} </if>
  </select>
</mapper>
```

约定：`<trim prefix="where" prefixOverrides="and">` 包动态条件；模糊查询 `like concat('%',#{x},'%')`；
集合用 `<foreach>`；`limit ${limit}`（`$` 拼接）；简单查询 `resultType="map"` + 列别名驼峰。
根 POM `<resources>` 已配扫描 `src/main/java` 下 `**/*.xml`，但实际统一放 `resources/mapper`。

### 4.7 对象转换（MapStruct）

```java
@Mapper
public interface PlatformManageConvert {
    PlatformManageConvert INSTANCE = Mappers.getMapper(PlatformManageConvert.class);

    Platform convert(PlatformSaveParam param);
    PlatformDetailVo convert(Platform entity);
    List<FsOptionVo> convertOptions(List<Platform> entities);   // 需 default 方法处理 Long→String
}
```

方法名统一 `convert`（重载区分方向）+ `convertOptions`（列表转下拉）。
**禁止**手写 getter/setter 拷贝，**禁止**用 `BeanUtils.copyProperties` 做主转换。
根 POM `annotationProcessorPaths` 已配 lombok → mapstruct-processor，**顺序不可调**。

### 4.8 Feign 契约

```java
@FeignClient(value = "bean-server", contextId = "bean-server",
             path = "/api/bean/department", configuration = FeignConfiguration.class)
public interface DepartmentServiceFeign {

    @RequestMapping(value = "page", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    PageResult page(@Validated @RequestBody DepartmentParam param);

    @RequestMapping(value = "/detail", consumes = "application/json", method = RequestMethod.GET)
    RestResult<?> detail(@RequestParam("id") Long id);
}
```

- `value` = 目标应用名 `{module}-server`；`path` = 目标 Controller `@RequestMapping` 根路径
- 必须指定 `configuration = FeignConfiguration.class`
- 用 `@RequestMapping` 而非 `@GetMapping`（历史约定）
- 返回类型复用 `RestResult`/`PageResult`，入参复用 entity 模块 Param
- client 层 Service 仅做转发，可加 `@GlobalTransactional`（Seata）

