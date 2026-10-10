package cn.com.nla.callcenter;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import cn.com.nla.callcenter.domain.*;
import cn.com.nla.callcenter.mapper.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import jakarta.validation.Validation;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** 执行交付 DDL 和真实 Mapper SQL；不启动 SIP/ZLM/Redis 或 Spring 上下文。 */
class CallcenterDataContractTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final List<String> PRIVATE_FIELDS = List.of("password", "passwd", "sipPwd", "secret", "secretKey", "notifyUrl", "cdrNotifyUrl");
    private JdbcDataSource dataSource;
    private SqlSession session;

    static Stream<Class<? extends BaseEntity>> entities() {
        return Stream.of(FsMediaServer.class, Platform.class, GateWay.class, RouteGroup.class, RouteGatewayGroup.class, RouteGateway.class, RouteCall.class, Company.class, CompanyConference.class, CompanyDisplay.class, CompanyPhoneGroup.class, CompanyPhone.class, Group.class, GroupAgentStrategy.class, GroupStrategyExp.class, GroupMemoryConfig.class, GroupOverflow.class, Skill.class, SkillAgent.class, SkillGroup.class, Agent.class, UserAgent.class, AgentGroup.class, AgentSip.class, AgentStateLog.class, CallDetail.class, CallDevice.class, CallDtmf.class, CallLog.class, GroupMemory.class, IvrWorkflow.class, OverflowConfig.class, OverflowExp.class, OverflowFront.class, Playback.class, PushLog.class, VdnSchedule.class, VdnCode.class, VdnPhone.class, VdnConfig.class, VdnDtmf.class);
    }

    static Stream<Class<? extends BaseEntity>> uniqueEntities() {
        return Stream.of(FsMediaServer.class, RouteGateway.class, Company.class, CompanyConference.class, CompanyPhone.class, Group.class, GroupMemoryConfig.class, GroupOverflow.class, Skill.class, Agent.class, AgentGroup.class, AgentSip.class, CallLog.class, GroupMemory.class, OverflowConfig.class, VdnPhone.class);
    }

    @BeforeEach
    void prepareDatabaseAndMappers() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:callcenter_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_callcenter.sql")) {
            assertNotNull(input, "测试必须加载交付 DDL");
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(ddl).doesNotContain("auto_increment", "INSERT INTO", "DROP TABLE")
            .contains("collate=utf8mb4_cs_0900_ai_ci");
        // H2 无 MySQL 表级存储选项、STORED 关键字；表达式及 NULL 唯一索引语义保留。
        ddl = ddl.replaceAll("(?im)^--[^\\r\\n]*", "")
            .replaceAll("(?i)\\) engine=innodb[^;]*;", ");")
            .replaceAll("(?i)\\bstored\\b", "");
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            for (String sql : ddl.split("(?m);\\s*$")) {
                if (!sql.isBlank()) {
                    // MySQL 索引名属于各表，H2 索引名属于 schema；仅在测试中加表名前缀。
                    var table = java.util.regex.Pattern.compile("create table if not exists (\\w+)").matcher(sql);
                    assertTrue(table.find());
                    sql = sql.replaceAll("(?i)\\bkey (\\w+)\\s*\\(", "key " + table.group(1) + "_$1 (");
                    statement.execute(sql);
                }
            }
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("callcenter-test", new JdbcTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig())
            .setMetaObjectHandler(new AuditFill()));
        for (Class<? extends BaseEntity> entity : entities().toList()) {
            configuration.addMapper(mapperClass(entity));
        }
        for (String name : List.of("Agent", "GroupOverflow")) {
            String resource = "mapper/callcenter/" + name + "Mapper.xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
    }

    @AfterEach
    void closeDatabase() throws Exception {
        if (session != null) { session.close(); }
        if (dataSource != null) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("shutdown");
            }
        }
    }

    @ParameterizedTest(name = "{0} 全字段持久化、更新审计、逻辑删除")
    @MethodSource("entities")
    void allColumnsSurviveCrudAndDeletionDoesNotRestoreRows(Class<? extends BaseEntity> type) throws Exception {
        BaseEntity entity = populated(type);
        BaseMapper<BaseEntity> mapper = mapper(type);
        assertEquals(1, mapper.insert(entity));
        var info = TableInfoHelper.getTableInfo(type);
        Serializable key = (Serializable) info.getPropertyValue(entity, info.getKeyProperty());
        assertNotNull(key);
        if (key instanceof Long id) { assertTrue(id > Integer.MAX_VALUE); }
        BaseEntity loaded = mapper.selectById(key);
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("delFlag")) { continue; }
            field.setAccessible(true);
            assertEquals(field.get(entity), field.get(loaded), type.getSimpleName() + "." + field.getName());
        }
        assertEquals("0", info.getPropertyValue(loaded, "delFlag"));
        assertEquals(10L, loaded.getCreateDept()); assertEquals(20L, loaded.getCreateBy());
        assertEquals(CREATED, loaded.getCreateTime()); assertEquals(CREATED, loaded.getUpdateTime());
        // 用部分实体更新，不能丢失未提供的业务列或创建审计。
        BaseEntity update = type.getDeclaredConstructor().newInstance();
        info.setPropertyValue(update, info.getKeyProperty(), key);
        Field changed = Stream.of(type.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.getName().equals(info.getKeyProperty())
                && !f.getName().equals("delFlag")).findFirst().orElseThrow();
        changed.setAccessible(true);
        Object changedValue = changed.getType() == String.class ? "更新中文" : 9_007_199_254_741_111L;
        changed.set(update, changedValue);
        assertEquals(1, mapper.updateById(update));
        BaseEntity afterUpdate = mapper.selectById(key);
        assertEquals(changedValue, changed.get(afterUpdate));
        assertEquals(CREATED, afterUpdate.getCreateTime()); assertEquals(20L, afterUpdate.getCreateBy());
        assertEquals(21L, afterUpdate.getUpdateBy()); assertEquals(CREATED.plusMinutes(1), afterUpdate.getUpdateTime());
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.equals(changed) || field.getName().equals("delFlag")) { continue; }
            field.setAccessible(true); assertEquals(field.get(loaded), field.get(afterUpdate), field.getName());
        }
        assertEquals(1, mapper.deleteById(key)); assertNull(mapper.selectById(key));
        assertThat(mapper.selectList(null)).isEmpty(); assertEquals(0L, mapper.selectCount(null));
        assertEquals(0, mapper.updateById(update)); assertEquals(0, mapper.deleteById(key));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("select del_flag from " + info.getTableName())) {
            assertTrue(rows.next()); assertEquals("1", rows.getString(1)); assertFalse(rows.next());
        }
    }

    @ParameterizedTest(name = "{0} 生成映射和输入校验")
    @MethodSource("entities")
    void generatedMappingsExcludeSecretsAuditAndDeleteInputs(Class<? extends BaseEntity> type) throws Exception {
        String name = type.getSimpleName();
        Class<?> boType = Class.forName("cn.com.nla.callcenter.domain.bo." + name + "Bo");
        Object bo = populated(boType);
        Object converted = convert("cn.com.nla.callcenter.domain.bo." + name + "BoTo" + name + "Mapper", bo);
        BaseEntity entity = (BaseEntity) converted;
        assertNull(entity.getCreateBy()); assertNull(entity.getCreateTime());
        var info = TableInfoHelper.getTableInfo(type);
        assertNull(info.getPropertyValue(entity, "delFlag"));
        for (Field field : boType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) { continue; }
            field.setAccessible(true);
            assertEquals(field.get(bo), info.getPropertyValue(entity, field.getName()), field.getName());
        }
        entity.setCreateTime(CREATED);
        Object view = convert("cn.com.nla.callcenter.domain." + name + "To" + name + "VoMapper", entity);
        String json = JsonMapper.builder().build().writeValueAsString(view);
        assertThat(json).doesNotContain("delFlag");
        for (String field : PRIVATE_FIELDS) {
            assertThat(json).doesNotContain("\"" + field + "\":");
            try {
                Field secret = boType.getDeclaredField(field); secret.setAccessible(true);
                assertThat(bo.toString()).doesNotContain(secret.get(bo).toString());
                assertThat(entity.toString()).doesNotContain(secret.get(bo).toString());
                assertThat(JsonMapper.builder().build().writeValueAsString(entity)).doesNotContain(secret.get(bo).toString());
                assertThat(JsonMapper.builder().build().writeValueAsString(bo)).doesNotContain(secret.get(bo).toString());
            } catch (NoSuchFieldException ignored) { /* 此表没有该凭据。 */ }
        }
        assertEquals(CREATED, view.getClass().getMethod("getCreateTime").invoke(view));
        for (Field field : view.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("sipPhoneList")) { continue; }
            field.setAccessible(true);
            String getter = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
            assertEquals(type.getMethod(getter).invoke(entity), field.get(view), name + "Vo." + field.getName());
        }
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(bo, AddGroup.class)).isEmpty();
            Field id = boType.getDeclaredField(info.getKeyProperty()); id.setAccessible(true);
            if (id.getType() == Long.class) {
                assertThat(validator.validate(bo, EditGroup.class)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactly(info.getKeyProperty());
                id.set(bo, 3_000_000_000L);
                assertThat(validator.validate(bo, EditGroup.class)).isEmpty();
                assertThat(validator.validate(bo, AddGroup.class)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactly(info.getKeyProperty());
            } else {
                assertThat(validator.validate(bo, EditGroup.class)).isEmpty();
                id.set(bo, " ");
                assertThat(validator.validate(bo, AddGroup.class)).extracting(v -> v.getPropertyPath().toString())
                    .contains(info.getKeyProperty());
            }
        }
    }

    @ParameterizedTest(name = "{0} 有效业务键唯一且支持多次删除重建")
    @MethodSource("uniqueEntities")
    void activeUniqueKeysAllowRepeatedDeleteAndRecreate(Class<? extends BaseEntity> type) throws Exception {
        var mapper = mapper(type); var info = TableInfoHelper.getTableInfo(type);
        for (int cycle = 0; cycle < 3; cycle++) {
            BaseEntity entity = populated(type);
            if (type == FsMediaServer.class) { ((FsMediaServer) entity).setId("ZLM-" + cycle); }
            mapper.insert(entity);
            BaseEntity duplicate = populated(type);
            if (type == FsMediaServer.class) { ((FsMediaServer) duplicate).setId("ZLM-duplicate-" + cycle); }
            assertThrows(RuntimeException.class, () -> mapper.insert(duplicate));
            assertEquals(1L, mapper.selectCount(null));
            mapper.deleteById((Serializable) info.getPropertyValue(entity, info.getKeyProperty()));
        }
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("select count(*) from " + info.getTableName()
                 + " where del_flag = '1' and active_marker is null")) {
            assertTrue(rows.next()); assertEquals(3, rows.getInt(1));
        }
    }

    @Test
    void stringPrimaryKeysRetainIdentityAfterLogicalDeletion() throws Exception {
        for (var type : List.of(FsMediaServer.class)) {
            var mapper = mapper(type); var entity = populated(type); mapper.insert(entity);
            String key = (String) TableInfoHelper.getTableInfo(type).getPropertyValue(entity, "id");
            assertTrue(key.contains("中文")); mapper.deleteById(key);
            assertThrows(RuntimeException.class, () -> mapper.insert(populated(type)));
        }
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> multipleUniqueKeys() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(Company.class, "name", "companyCode"),
            org.junit.jupiter.params.provider.Arguments.of(Company.class, "companyCode", "name"),
            org.junit.jupiter.params.provider.Arguments.of(CompanyConference.class, "name", "code"),
            org.junit.jupiter.params.provider.Arguments.of(CompanyConference.class, "code", "name"),
            org.junit.jupiter.params.provider.Arguments.of(Agent.class, "agentKey", "agentCode"),
            org.junit.jupiter.params.provider.Arguments.of(Agent.class, "agentCode", "agentKey"));
    }

    @ParameterizedTest(name = "{0}.{1} 独立唯一键冲突与删除重建")
    @MethodSource("multipleUniqueKeys")
    void eachUniqueKeyIndependentlyRejectsDuplicates(Class<? extends BaseEntity> type,
                                                    String collidingProperty, String differentProperty) throws Exception {
        var mapper = mapper(type); var info = TableInfoHelper.getTableInfo(type);
        Field different = type.getDeclaredField(differentProperty); different.setAccessible(true);
        Field colliding = type.getDeclaredField(collidingProperty); colliding.setAccessible(true);
        for (int cycle = 0; cycle < 3; cycle++) {
            var original = populated(type); mapper.insert(original);
            var duplicate = populated(type); different.set(duplicate, "distinct-" + cycle);
            assertThrows(RuntimeException.class, () -> mapper.insert(duplicate));
            assertEquals(1L, mapper.selectCount(null));
            var distinct = populated(type); different.set(distinct, "distinct-" + cycle);
            colliding.set(distinct, "distinct-" + cycle); mapper.insert(distinct);
            assertEquals(2L, mapper.selectCount(null));
            assertEquals(1, mapper.deleteById((Serializable) info.getPropertyValue(distinct, "id")));
            assertEquals(1, mapper.deleteById((Serializable) info.getPropertyValue(original, "id")));
        }
    }

    @Test
    void companyBalanceKeepsExactDecimalAndForgottenSqlColumnsRoundTrip() {
        var company = new Company(); company.setName("企业"); company.setCompanyCode("CODE");
        company.setBalance(new java.math.BigDecimal("123456789012345.1234")); company.setConferenceLimit(36);
        var mapper = session.getMapper(CompanyMapper.class); mapper.insert(company);
        var loaded = mapper.selectById(company.getId());
        assertEquals(company.getBalance(), loaded.getBalance()); assertEquals(36, loaded.getConferenceLimit());
        var agent = new Agent(); agent.setAgentKey("agent"); agent.setAgentCode("001"); agent.setDisplay("13812348000");
        var agents = session.getMapper(AgentMapper.class); agents.insert(agent);
        assertEquals("13812348000", agents.selectById(agent.getId()).getDisplay());
    }

    @Test
    void epochCountersScheduleStringsAndMixedRouteValuesKeepOriginalSemantics() {
        var log = new CallLog(); log.setCallId(9_007_199_254_740_993L);
        log.setCallTime(1_792_000_000_123L); log.setTalkTime(75L); log.setFristQueueTime(1_792_000_000_000L);
        var logs = session.getMapper(CallLogMapper.class); logs.insert(log);
        var loaded = logs.selectById(log.getId());
        assertEquals(log.getCallTime(), loaded.getCallTime()); assertEquals(75L, loaded.getTalkTime());
        assertEquals(log.getFristQueueTime(), loaded.getFristQueueTime());
        var schedule = new VdnSchedule(); schedule.setCompanyId(9_007_199_254_740_993L);
        schedule.setStartDay("2026-10-10"); schedule.setEndDay("2026-12-31");
        schedule.setStartTime("08:30:00"); schedule.setEndTime("17:30:00"); schedule.setSun(1);
        var schedules = session.getMapper(VdnScheduleMapper.class); schedules.insert(schedule);
        var loadedSchedule = schedules.selectById(schedule.getId());
        assertEquals("08:30:00", loadedSchedule.getStartTime()); assertEquals("2026-10-10", loadedSchedule.getStartDay());
        var config = new VdnConfig(); config.setRouteValue("+8613812348000");
        var configs = session.getMapper(VdnConfigMapper.class); configs.insert(config);
        assertEquals(config.getRouteValue(), configs.selectById(config.getId()).getRouteValue());
    }

    @Test
    void userAndSipLookupRespectCompanyScopeAndExactSnowflakeIds() {
        var fixture = directory(); var agents = session.getMapper(AgentMapper.class);
        var found = agents.selectByUserId(fixture.company.getId(), fixture.binding.getUserId());
        assertThat(found).hasSize(1); assertEquals(fixture.agent.getId(), found.getFirst().getId());
        assertEquals(fixture.agent.getId(), agents.selectBySip(fixture.company.getId(), fixture.sip.getSip()).getId());
        assertThat(agents.selectByUserId(fixture.company.getId(), fixture.binding.getUserId() + 1)).isEmpty();
        assertThat(agents.selectByUserId(null, fixture.binding.getUserId())).isEmpty();
        assertNull(agents.selectBySip(fixture.company.getId(), "' OR 1=1 --"));
        assertNull(agents.selectBySip(fixture.company.getId(), null));
        assertNull(agents.selectBySip(null, fixture.sip.getSip()));
        // 字符串与BIGINT隐式比较会丢失雪花ID精度；新关联字段必须是Long。
        assertTrue(fixture.binding.getUserId() > 9_007_199_254_740_992L);
        var other = company("另一个企业");
        var invalid = new UserAgent(); invalid.setCompanyId(other.getId()); invalid.setAgentId(fixture.agent.getId());
        invalid.setUserId(fixture.binding.getUserId()); session.getMapper(UserAgentMapper.class).insert(invalid);
        assertThat(agents.selectByUserId(other.getId(), fixture.binding.getUserId())).isEmpty();
        var invalidSip = new AgentSip(); invalidSip.setCompanyId(other.getId()); invalidSip.setAgentId(fixture.agent.getId());
        invalidSip.setSip("错企业分机"); session.getMapper(AgentSipMapper.class).insert(invalidSip);
        assertNull(agents.selectBySip(other.getId(), invalidSip.getSip()));
        assertNull(agents.selectBySip(fixture.company.getId(), invalidSip.getSip()));
        assertThat(JsonMapper.builder().build().writeValueAsString(found)).doesNotContain("passwd", "sipPwd");
    }

    static Stream<String> directoryParts() { return Stream.of("company", "agent", "binding", "sip"); }

    @ParameterizedTest(name = "删除 {0} 后用户/SIP查询过滤")
    @MethodSource("directoryParts")
    void directoryQueriesFilterEveryJoinedDeletion(String part) throws Exception {
        var fixture = directory();
        BaseEntity entity = (BaseEntity) fixture.getClass().getDeclaredField(part).get(fixture);
        var info = TableInfoHelper.getTableInfo(entity.getClass());
        mapper(entity.getClass()).deleteById((Serializable) info.getPropertyValue(entity, "id"));
        var agents = session.getMapper(AgentMapper.class);
        assertThat(agents.selectByUserId(fixture.company.getId(), fixture.binding.getUserId())).hasSize(part.equals("sip") ? 1 : 0);
        assertEquals(part.equals("binding"), agents.selectBySip(fixture.company.getId(), fixture.sip.getSip()) != null);
    }

    @Test
    void duplicateUserBindingsDoNotDuplicateAgentsAndNewestAgentOrderIsStable() {
        var fixture = directory(); var bindings = session.getMapper(UserAgentMapper.class);
        var duplicate = new UserAgent(); duplicate.setCompanyId(fixture.company.getId()); duplicate.setAgentId(fixture.agent.getId());
        duplicate.setUserId(fixture.binding.getUserId()); bindings.insert(duplicate);
        assertThat(session.getMapper(AgentMapper.class).selectByUserId(fixture.company.getId(), fixture.binding.getUserId())).hasSize(1);
        var second = new Agent(); second.setCompanyId(fixture.company.getId()); second.setAgentKey("newest"); second.setAgentCode("002");
        var agents = session.getMapper(AgentMapper.class); agents.insert(second);
        var binding = new UserAgent(); binding.setCompanyId(fixture.company.getId()); binding.setAgentId(second.getId());
        binding.setUserId(fixture.binding.getUserId()); bindings.insert(binding);
        assertThat(agents.selectByUserId(fixture.company.getId(), fixture.binding.getUserId())).extracting(v -> v.getId())
            .containsExactly(second.getId(), fixture.agent.getId());
    }

    @Test
    void overflowQueryScopesCompanyAndGroupAndReturnsConfigurationIdInPriorityOrder() {
        var fixture = overflow();
        var second = new OverflowConfig(); second.setCompanyId(fixture.company.getId()); second.setName("优先策略");
        second.setOverflowValue(9_007_199_254_740_999L); session.getMapper(OverflowConfigMapper.class).insert(second);
        var link = new GroupOverflow(); link.setGroupId(fixture.group.getId()); link.setOverflowId(second.getId()); link.setLevelValue(1);
        session.getMapper(GroupOverflowMapper.class).insert(link);
        var mapper = session.getMapper(GroupOverflowMapper.class);
        var configs = mapper.selectConfigsByGroup(fixture.company.getId(), fixture.group.getId());
        assertThat(configs).extracting(v -> v.getId()).containsExactly(second.getId(), fixture.config.getId());
        assertEquals(9_007_199_254_740_999L, configs.getFirst().getOverflowValue());
        assertThat(mapper.selectConfigsByGroup(null, fixture.group.getId())).isEmpty();
        assertThat(mapper.selectConfigsByGroup(fixture.company.getId(), null)).isEmpty();
        var other = company("错误企业");
        assertThat(mapper.selectConfigsByGroup(other.getId(), fixture.group.getId())).isEmpty();
        var update = new OverflowConfig(); update.setId(second.getId()); update.setCompanyId(other.getId());
        session.getMapper(OverflowConfigMapper.class).updateById(update);
        assertThat(mapper.selectConfigsByGroup(fixture.company.getId(), fixture.group.getId())).extracting(v -> v.getId())
            .containsExactly(fixture.config.getId());
    }

    static Stream<String> overflowParts() { return Stream.of("company", "group", "config", "link"); }

    @ParameterizedTest(name = "删除 {0} 后溢出查询过滤")
    @MethodSource("overflowParts")
    void overflowQueryFiltersEveryJoinedDeletion(String part) throws Exception {
        var fixture = overflow();
        BaseEntity entity = (BaseEntity) fixture.getClass().getDeclaredField(part).get(fixture);
        mapper(entity.getClass()).deleteById(entity.getClass().getMethod("getId").invoke(entity).toString());
        assertThat(session.getMapper(GroupOverflowMapper.class).selectConfigsByGroup(fixture.company.getId(), fixture.group.getId())).isEmpty();
    }

    @Test
    void phoneMaskingUsesExistingResponseEnhancerWithoutChangingDatabaseOrDto() throws Exception {
        var company = company("脱敏企业"); company.setPhone("13812348000"); session.getMapper(CompanyMapper.class).updateById(company);
        var loaded = session.getMapper(CompanyMapper.class).selectById(company.getId());
        var view = (cn.com.nla.callcenter.domain.vo.CompanyVo) convert("cn.com.nla.callcenter.domain.CompanyToCompanyVoMapper", loaded);
        var processor = new cn.com.nla.common.sensitive.handler.SensitiveJsonFieldProcessor();
        org.springframework.test.util.ReflectionTestUtils.setField(processor, "sensitiveService",
            (cn.com.nla.common.sensitive.core.SensitiveService) (roles, perms) -> true);
        var json = JsonMapper.builder().build();
        var enhancer = new cn.com.nla.common.json.enhance.JsonValueEnhancer(json, List.of(processor));
        String masked = json.writeValueAsString(enhancer.enhance(view));
        assertThat(masked).contains("138****8000").doesNotContain("13812348000", "secretKey", "notifyUrl");
        assertEquals("13812348000", view.getPhone()); assertEquals("13812348000", loaded.getPhone());
        assertThat(view.toString()).doesNotContain("13812348000");
    }

    @Test
    void validationAndDatabaseRejectInvalidScalePortAndUnboundStringPrimaryKey() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var bo = new cn.com.nla.callcenter.domain.bo.CompanyBo(); bo.setBalance(new java.math.BigDecimal("1.12345"));
            assertThat(validator.validate(bo, AddGroup.class)).extracting(v -> v.getPropertyPath().toString()).contains("balance");
            var platform = new cn.com.nla.callcenter.domain.bo.PlatformBo(); platform.setInternalPort(65536); platform.setStartRtpPort(-1);
            assertThat(validator.validate(platform, AddGroup.class)).extracting(v -> v.getPropertyPath().toString())
                .contains("internalPort", "startRtpPort");
            var link = new cn.com.nla.callcenter.domain.bo.UserAgentBo();
            assertThat(validator.validate(link, AddGroup.class)).extracting(v -> v.getPropertyPath().toString()).contains("userId", "agentId");
        }
        var media = (FsMediaServer) populated(FsMediaServer.class); media.setId(null);
        assertThrows(RuntimeException.class, () -> session.getMapper(FsMediaServerMapper.class).insert(media));
        var tooLong = new AgentSip(); tooLong.setSip("x".repeat(256));
        assertThrows(RuntimeException.class, () -> session.getMapper(AgentSipMapper.class).insert(tooLong));
        var badLink = new UserAgent();
        assertThrows(RuntimeException.class, () -> session.getMapper(UserAgentMapper.class).insert(badLink));
    }

    @Test
    void credentialJsonInputsRemainWriteOnly() {
        var json = JsonMapper.builder().build();
        var agent = json.readValue("{\"agentKey\":\"test\",\"passwd\":\"test-only-password\"}", cn.com.nla.callcenter.domain.bo.AgentBo.class);
        assertEquals("test-only-password", agent.getPasswd());
        assertThat(json.writeValueAsString(agent)).doesNotContain("passwd", "test-only-password");
        var sip = json.readValue("{\"sip\":\"1001\",\"sipPwd\":\"test-only-sip-password\"}", cn.com.nla.callcenter.domain.bo.AgentSipBo.class);
        assertEquals("test-only-sip-password", sip.getSipPwd());
        assertThat(sip.toString()).doesNotContain("test-only-sip-password");
    }

    private Company company(String name) {
        var company = new Company(); company.setName(name); company.setCompanyCode(name); session.getMapper(CompanyMapper.class).insert(company);
        return company;
    }

    private Directory directory() {
        var company = company("目录企业");
        var agent = new Agent(); agent.setCompanyId(company.getId()); agent.setAgentKey("agent-key"); agent.setAgentCode("001");
        agent.setPasswd("internal-password"); session.getMapper(AgentMapper.class).insert(agent);
        var binding = new UserAgent(); binding.setCompanyId(company.getId()); binding.setAgentId(agent.getId());
        binding.setUserId(9_007_199_254_740_993L); session.getMapper(UserAgentMapper.class).insert(binding);
        var sip = new AgentSip(); sip.setCompanyId(company.getId()); sip.setAgentId(agent.getId()); sip.setSip("1001"); sip.setSipPwd("internal-sip-password");
        session.getMapper(AgentSipMapper.class).insert(sip);
        return new Directory(company, agent, binding, sip);
    }

    private Overflow overflow() {
        var company = company("溢出企业");
        var group = new Group(); group.setCompanyId(company.getId()); group.setName("技能组"); session.getMapper(GroupMapper.class).insert(group);
        var config = new OverflowConfig(); config.setCompanyId(company.getId()); config.setName("策略"); config.setOverflowValue(9_007_199_254_740_993L);
        session.getMapper(OverflowConfigMapper.class).insert(config);
        var link = new GroupOverflow(); link.setGroupId(group.getId()); link.setOverflowId(config.getId()); link.setLevelValue(10);
        session.getMapper(GroupOverflowMapper.class).insert(link);
        return new Overflow(company, group, config, link);
    }

    private record Directory(Company company, Agent agent, UserAgent binding, AgentSip sip) { }
    private record Overflow(Company company, Group group, OverflowConfig config, GroupOverflow link) { }

    @SuppressWarnings("unchecked")
    private static Class<BaseMapper<BaseEntity>> mapperClass(Class<?> entity) throws ClassNotFoundException {
        return (Class<BaseMapper<BaseEntity>>) Class.forName("cn.com.nla.callcenter.mapper." + entity.getSimpleName() + "Mapper");
    }

    private BaseMapper<BaseEntity> mapper(Class<?> entity) throws ClassNotFoundException {
        return session.getMapper(mapperClass(entity));
    }

    private static <T> T populated(Class<T> type) throws Exception {
        T object = type.getDeclaredConstructor().newInstance();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("delFlag")) { continue; }
            field.setAccessible(true);
            if (field.getType() == String.class) { field.set(object, PRIVATE_FIELDS.contains(field.getName()) ? "private-" + field.getName() : "中文-" + field.getName()); }
            else if (field.getType() == Integer.class) { field.set(object, 7); }
            else if (field.getType() == Long.class && !field.getName().equals("id")) { field.set(object, 9_007_199_254_740_993L); }
            else if (field.getType() == java.math.BigDecimal.class) { field.set(object, new java.math.BigDecimal("123456789.1234")); }
            else if (field.getType() == LocalDateTime.class) { field.set(object, CREATED); }
        }
        return object;
    }

    private static Object convert(String mapperName, Object source) throws Exception {
        Object generated = Mappers.getMapper(Class.forName(mapperName));
        return generated.getClass().getMethod("convert", source.getClass()).invoke(generated, source);
    }

    private static class AuditFill implements MetaObjectHandler {
        @Override public void insertFill(MetaObject object) {
            strictInsertFill(object, "createDept", Long.class, 10L);
            strictInsertFill(object, "createBy", Long.class, 20L);
            strictInsertFill(object, "createTime", LocalDateTime.class, CREATED);
            strictInsertFill(object, "updateBy", Long.class, 20L);
            strictInsertFill(object, "updateTime", LocalDateTime.class, CREATED);
        }
        @Override public void updateFill(MetaObject object) {
            strictUpdateFill(object, "updateBy", Long.class, 21L);
            strictUpdateFill(object, "updateTime", LocalDateTime.class, CREATED.plusMinutes(1));
        }
    }
}
