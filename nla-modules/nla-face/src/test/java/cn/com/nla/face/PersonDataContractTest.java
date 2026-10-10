package cn.com.nla.face;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.domain.PersonToPersonVoMapper;
import cn.com.nla.face.domain.bo.PersonBo;
import cn.com.nla.face.domain.bo.PersonBoToPersonMapper;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.mapper.PersonMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
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
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** 实际执行 Mapper SQL 和交付 DDL 的列定义，不加载 Spring/Redis/JNI。 */
class PersonDataContractTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 10, 8, 0);
    private JdbcDataSource dataSource;
    private SqlSession session;
    private PersonMapper mapper;

    @BeforeEach
    void prepareDatabaseAndMapper() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:face_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_face.sql")) {
            assertNotNull(input, "交付 DDL 必须作为测试资源加载");
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(ddl).contains("collate=utf8mb4_cs_0900_ai_ci");
        // H2 不实现 MySQL 的存储引擎、字符集、排序规则和行格式，只剥离表级尾部选项。
        ddl = ddl.replaceFirst("(?is)\\) engine=innodb.*;\\s*$", ");");
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(ddl);
        }

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("face-test", new JdbcTransactionFactory(), dataSource));
        var global = new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig())
            .setMetaObjectHandler(new AuditFill());
        GlobalConfigUtils.setGlobalConfig(configuration, global);
        String resource = "mapper/face/PersonMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        mapper = session.getMapper(PersonMapper.class);
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

    @Test
    void snowflakeAuditAndLargeFeatureSurvivePersistenceWithoutOrdinaryQueryLoadingFeature() {
        Person person = person("中文图像");
        String features = "[" + "0.123456,".repeat(1023) + "0.123456]";
        person.setExtract(features);
        assertEquals(1, mapper.insert(person));
        assertNotNull(person.getId()); assertTrue(person.getId() > Integer.MAX_VALUE);
        Person loaded = mapper.selectById(person.getId());
        assertEquals("张三", loaded.getPersonName()); assertEquals("中文图像", loaded.getImgId());
        assertNull(loaded.getExtract()); assertEquals("0", loaded.getDelFlag());
        assertEquals(10L, loaded.getCreateDept()); assertEquals(20L, loaded.getCreateBy());
        assertEquals(CREATED, loaded.getCreateTime()); assertEquals(CREATED, loaded.getUpdateTime());
        assertEquals(features, mapper.selectFeatureList().getFirst().getExtract());
    }

    @Test
    void imageLookupUsesBoundParametersAndDoesNotReturnUnrequestedRowsOrFeatures() {
        Person requested = person("图像' OR 1=1 --"); Person other = person("其他图像");
        mapper.insert(requested); mapper.insert(other);
        var found = mapper.selectImgIdList(List.of(requested.getImgId()));
        assertThat(found).extracting(Person::getId).containsExactly(requested.getId());
        assertNull(found.getFirst().getExtract()); assertEquals(CREATED, found.getFirst().getCreateTime());
    }

    @Test
    void nullEmptyAndUnknownImageIdsReturnEmptyResults() {
        mapper.insert(person("存在的图像"));
        assertThat(mapper.selectImgIdList(null)).isEmpty();
        assertThat(mapper.selectImgIdList(List.of())).isEmpty();
        assertThat(mapper.selectImgIdList(List.of("不存在的图像"))).isEmpty();
    }

    @Test
    void logicalDeleteRetainsPhysicalRowAndExcludesItFromAllReadPaths() throws Exception {
        Person person = person("同一图像"); mapper.insert(person);
        assertEquals(1, mapper.deleteById(person.getId()));
        assertNull(mapper.selectById(person.getId()));
        assertThat(mapper.selectImgIdList(List.of(person.getImgId()))).isEmpty();
        assertThat(mapper.selectFeatureList()).isEmpty();
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("select del_flag from face_person")) {
            assertTrue(rows.next()); assertEquals("1", rows.getString(1)); assertFalse(rows.next());
        }
        Person replacement = person("同一图像"); mapper.insert(replacement);
        assertThat(mapper.selectImgIdList(List.of("同一图像"))).extracting(Person::getId)
            .containsExactly(replacement.getId());
    }

    @Test
    void profileUpdatePreservesFeaturesAndCreationAuditAndFillsUpdateAudit() {
        Person person = person("图像"); mapper.insert(person);
        Person update = new Person(); update.setId(person.getId()); update.setPersonName("李四");
        assertEquals(1, mapper.updateById(update));
        Person loaded = mapper.selectById(person.getId());
        assertEquals("李四", loaded.getPersonName()); assertEquals(CREATED, loaded.getCreateTime());
        assertEquals(21L, loaded.getUpdateBy()); assertEquals(CREATED.plusMinutes(1), loaded.getUpdateTime());
        assertEquals("[0.1,0.2]", mapper.selectFeatureList().getFirst().getExtract());
    }

    @Test
    void internalFeatureQuerySkipsEmptyFeaturesAndOnlyLoadsFeatureColumns() {
        Person ready = person("已提取"); Person empty = person("未提取"); empty.setExtract("");
        mapper.insert(ready); mapper.insert(empty);
        var features = mapper.selectFeatureList();
        assertThat(features).extracting(Person::getId).containsExactly(ready.getId());
        assertEquals("已提取", features.getFirst().getImgId());
        assertNull(features.getFirst().getPersonName()); assertNull(features.getFirst().getImgUrl());
    }

    @Test
    void generatedMappersPreservePublicFieldsAndExcludeInternalFeaturesFromViewJson() {
        PersonBo bo = validBo();
        Person entity = Mappers.getMapper(PersonBoToPersonMapper.class).convert(bo);
        assertEquals("中文编号", entity.getImgId()); assertEquals("张三", entity.getPersonName());
        assertNull(entity.getExtract()); assertNull(entity.getDelFlag()); assertNull(entity.getCreateBy());
        entity.setId(3_000_000_000L); entity.setExtract("[0.1,0.2]"); entity.setDelFlag("0");
        entity.setCreateTime(CREATED);
        PersonVo view = Mappers.getMapper(PersonToPersonVoMapper.class).convert(entity);
        assertEquals(entity.getId(), view.getId()); assertEquals(CREATED, view.getCreateTime());
        String json = JsonMapper.builder().build().writeValueAsString(view);
        assertThat(json).contains("张三").doesNotContain("extract", "delFlag", "[0.1,0.2]");
        assertThat(entity.toString()).doesNotContain("[0.1,0.2]");
    }

    @Test
    void validationDistinguishesAddAndEditAndRejectsInvalidProfileFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator(); PersonBo bo = validBo();
            assertThat(validator.validate(bo, AddGroup.class)).isEmpty();
            assertThat(validator.validate(bo, EditGroup.class)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("id");
            bo.setId(3_000_000_000L);
            assertThat(validator.validate(bo, EditGroup.class)).isEmpty();
            assertThat(validator.validate(bo, AddGroup.class)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("id");
            bo.setPersonAge(-1); bo.setGender(3); bo.setImgId("a".repeat(41)); bo.setPersonName(" ");
            assertThat(validator.validate(bo, EditGroup.class)).extracting(v -> v.getPropertyPath().toString())
                .contains("personAge", "gender", "imgId", "personName");
        }
    }

    @Test
    void databaseRequiresFeaturesAndEnforcesProfileColumnLength() {
        Person missing = person("缺特征"); missing.setExtract(null);
        assertThrows(RuntimeException.class, () -> mapper.insert(missing));
        Person tooLong = person("a".repeat(41));
        assertThrows(RuntimeException.class, () -> mapper.insert(tooLong));
    }

    private static Person person(String imgId) {
        Person person = new Person(); person.setImgId(imgId); person.setImgUrl("/images/人脸.png");
        person.setExtract("[0.1,0.2]"); person.setPersonName("张三"); person.setPersonAge(30);
        person.setGender(1); person.setAddress("上海"); return person;
    }

    private static PersonBo validBo() {
        PersonBo bo = new PersonBo(); bo.setImgId("中文编号"); bo.setImgUrl("/images/人脸.png");
        bo.setPersonName("张三"); bo.setPersonAge(30); bo.setGender(1); bo.setAddress("上海"); return bo;
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
