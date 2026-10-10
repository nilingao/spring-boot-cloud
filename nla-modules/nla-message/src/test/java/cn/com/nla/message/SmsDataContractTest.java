package cn.com.nla.message;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.message.domain.MobileMessage;
import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.mapper.MobileMessageMapper;
import cn.com.nla.message.mapper.MobileMessageTemplateMapper;
import cn.com.nla.message.mapper.SmsConfigMapper;
import cn.com.nla.message.service.impl.MobileMessageTemplateServiceImpl;
import cn.com.nla.message.service.impl.SmsConfigServiceImpl;
import cn.com.nla.message.sms.config.DbSmsReadConfig;
import cn.com.nla.message.sms.core.SmsChannelManager;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 执行交付 DDL 和真实 Mapper/业务查询，无真实短信发送或 Redis 连接。 */
class SmsDataContractTest {
    private JdbcDataSource dataSource;
    private SqlSession session;
    private SmsConfigMapper configs;
    private MobileMessageTemplateMapper templates;
    private MobileMessageMapper records;
    private DbSmsReadConfig reader;
    private SmsChannelManager channels;

    @BeforeEach
    void prepareDatabase() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sms_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_message.sql")) {
            assertNotNull(input);
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var tables = Pattern.compile("(?is)create table sms_[a-z_]+\\s*\\(.*?\\) engine=innodb[^;]*;").matcher(ddl);
        int count = 0;
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            while (tables.find()) {
                String sql = tables.group();
                assertThat(sql).contains("collate=utf8mb4_cs_0900_ai_ci");
                // H2 不实现 MySQL 表级引擎/字符集/排序规则，仅剥离尾部选项，保留列定义。
                statement.execute(sql.replaceFirst("(?is)\\) engine=innodb[^;]*;", ");"));
                count++;
            }
        }
        assertEquals(3, count, "必须加载交付脚本的全部三张短信表，不执行菜单种子或 drop");
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("sms-test", new JdbcTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        var pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        configuration.addInterceptor(pagination);
        configuration.addMapper(SmsConfigMapper.class);
        configuration.addMapper(MobileMessageTemplateMapper.class);
        configuration.addMapper(MobileMessageMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        configs = session.getMapper(SmsConfigMapper.class);
        templates = session.getMapper(MobileMessageTemplateMapper.class);
        records = session.getMapper(MobileMessageMapper.class);
        reader = new DbSmsReadConfig(configs);
        channels = new SmsChannelManager(reader, configs, templates);
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
    void schemaAndEntityDefaultsUseSingleCharacterStringFlags() throws Exception {
        var config = config(3_000_000_001L, 1); var template = template(3_000_000_002L, config.getId());
        configs.insert(config); templates.insert(template);
        assertEquals("0", configs.selectById(config.getId()).getDelFlag());
        assertEquals("0", templates.selectById(template.getId()).getDelFlag());
        try (var connection = dataSource.getConnection()) {
            for (String table : List.of("SMS_SMS_CONFIG", "SMS_MOBILE_MESSAGE_TEMPLATE")) {
                try (var columns = connection.getMetaData().getColumns(null, null, table, "DEL_FLAG")) {
                    assertTrue(columns.next()); assertEquals(Types.CHAR, columns.getInt("DATA_TYPE"));
                    assertEquals(1, columns.getInt("COLUMN_SIZE")); assertEquals(0, columns.getInt("NULLABLE"));
                }
            }
        }
    }

    @Test
    void deletedEnabledChannelsAreNotReadBySupplierConfiguration() throws Exception {
        var deleted = config(3_000_000_001L, 1); var kept = config(3_000_000_002L, 1);
        configs.insert(deleted); configs.insert(kept);
        assertEquals(1, configs.deleteById(deleted.getId()));
        assertNull(reader.getSupplierConfig(String.valueOf(deleted.getId())));
        assertThat(reader.getSupplierConfigList()).extracting(c -> c.getConfigId()).containsExactly(String.valueOf(kept.getId()));
        assertEquals("1", storedFlag("sms_sms_config", deleted.getId()));
        assertEquals(2, physicalCount("sms_sms_config"));
    }

    @Test
    void deletedLatestTemplateFallsBackToLatestRemainingTemplate() {
        var config = config(3_000_000_001L, 1); configs.insert(config);
        var old = template(3_000_000_010L, config.getId()); var latest = template(3_000_000_011L, config.getId());
        templates.insert(old); templates.insert(latest);
        assertEquals(latest.getId(), channels.findLastTemplate(config.getId(), 1).getId());
        var service = new MobileMessageTemplateServiceImpl(templates);
        assertTrue(service.deleteWithValidByIds(List.of(latest.getId()), true));
        assertNull(templates.selectById(latest.getId()));
        assertEquals(old.getId(), channels.findLastTemplate(config.getId(), 1).getId());
        assertTrue(service.deleteWithValidByIds(List.of(old.getId()), true));
        assertNull(channels.findLastTemplate(config.getId(), 1));
    }

    @Test
    void activeChannelSelectionFiltersDeletedTemplatesDeletedConfigsAndInactiveConfigs() {
        var kept = config(3_000_000_001L, 1); var noTemplate = config(3_000_000_002L, 1);
        var removed = config(3_000_000_003L, 1); var inactive = config(3_000_000_004L, 0);
        for (var config : List.of(kept, noTemplate, removed, inactive)) { configs.insert(config); }
        var removedTemplate = template(3_000_000_012L, noTemplate.getId());
        templates.insert(template(3_000_000_011L, kept.getId())); templates.insert(removedTemplate);
        templates.insert(template(3_000_000_013L, removed.getId())); templates.insert(template(3_000_000_014L, inactive.getId()));
        templates.deleteById(removedTemplate.getId()); configs.deleteById(removed.getId());
        assertThat(channels.findActiveConfigs(1)).extracting(SmsConfig::getId).containsExactly(kept.getId());
        assertThat(channels.findActiveConfigs(2)).isEmpty();
    }

    @Test
    void referenceGuardBlocksDeletionUntilTemplateIsLogicallyDeletedAndUnregistersOnlyTarget() throws Exception {
        var config = config(3_000_000_001L, 1); var kept = config(3_000_000_002L, 1);
        configs.insert(config); configs.insert(kept);
        var template = template(3_000_000_011L, config.getId()); templates.insert(template);
        var manager = mock(SmsChannelManager.class);
        var service = new SmsConfigServiceImpl(configs, templates, manager);
        assertThrows(ServiceException.class, () -> service.deleteWithValidByIds(List.of(config.getId()), true));
        assertEquals("0", storedFlag("sms_sms_config", config.getId())); verifyNoInteractions(manager);
        templates.deleteById(template.getId());
        assertTrue(service.deleteWithValidByIds(List.of(config.getId()), true));
        assertEquals("1", storedFlag("sms_sms_config", config.getId()));
        assertNotNull(configs.selectById(kept.getId()));
        verify(manager).remove(config.getId()); verifyNoMoreInteractions(manager);
    }

    @Test
    void batchDeleteAndPaginationCountOnlyLiveRowsInBothTables() throws Exception {
        for (long id = 3_000_000_001L; id <= 3_000_000_003L; id++) {
            configs.insert(config(id, 1)); templates.insert(template(id + 10, id));
        }
        assertEquals(2, configs.deleteByIds(List.of(3_000_000_001L, 3_000_000_002L)));
        assertEquals(2, templates.deleteByIds(List.of(3_000_000_011L, 3_000_000_012L)));
        var configPage = configs.selectPage(new Page<>(1, 10), new LambdaQueryWrapper<SmsConfig>());
        var templatePage = templates.selectPage(new Page<>(1, 10), new LambdaQueryWrapper<MobileMessageTemplate>());
        assertEquals(1L, configPage.getTotal()); assertEquals(1L, templatePage.getTotal());
        assertThat(configPage.getRecords()).extracting(SmsConfig::getId).containsExactly(3_000_000_003L);
        assertThat(templatePage.getRecords()).extracting(MobileMessageTemplate::getId).containsExactly(3_000_000_013L);
        assertEquals(3, physicalCount("sms_sms_config")); assertEquals(3, physicalCount("sms_mobile_message_template"));
    }

    @Test
    void updatesAndRepeatedDeletesDoNotRestoreDeletedRows() throws Exception {
        var config = config(3_000_000_001L, 1); var template = template(3_000_000_011L, config.getId());
        configs.insert(config); templates.insert(template); configs.deleteById(config.getId()); templates.deleteById(template.getId());
        config.setConfigName("修改删除项"); config.setDelFlag("0"); template.setTitle("修改删除项"); template.setDelFlag("0");
        assertEquals(0, configs.updateById(config)); assertEquals(0, templates.updateById(template));
        assertEquals(0, configs.deleteById(config.getId())); assertEquals(0, templates.deleteById(template.getId()));
        assertEquals("1", storedFlag("sms_sms_config", config.getId()));
        assertEquals("1", storedFlag("sms_mobile_message_template", template.getId()));
    }

    @Test
    void sendingRecordsContinueToUsePhysicalDeletion() throws Exception {
        var record = new MobileMessage(); record.setId(3_000_000_001L); record.setMobile("13800000000");
        record.setContent("测试记录"); records.insert(record);
        assertEquals(1, physicalCount("sms_mobile_message"));
        assertEquals(1, records.deleteById(record.getId()));
        assertEquals(0, physicalCount("sms_mobile_message")); assertNull(records.selectById(record.getId()));
        try (var connection = dataSource.getConnection();
             var columns = connection.getMetaData().getColumns(null, null, "SMS_MOBILE_MESSAGE", "DEL_FLAG")) {
            assertFalse(columns.next());
        }
    }

    private static SmsConfig config(long id, int active) {
        var config = new SmsConfig(); config.setId(id); config.setSmsType(5); config.setConfigName("测试渠道");
        config.setAccount("test-account"); config.setPassword("test-only"); config.setIsActive(active); return config;
    }

    private static MobileMessageTemplate template(long id, long configId) {
        var template = new MobileMessageTemplate(); template.setId(id); template.setConfigId(configId);
        template.setType(1); template.setTitle("测试模板"); template.setCode("test-template"); template.setContent("测试内容"); return template;
    }

    private String storedFlag(String table, long id) throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("select del_flag from " + table + " where id = ?")) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) { assertTrue(rows.next()); return rows.getString(1); }
        }
    }

    private int physicalCount(String table) throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("select count(*) from " + table)) {
            assertTrue(rows.next()); return rows.getInt(1);
        }
    }
}
