package cn.com.nla.video;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import cn.com.nla.video.domain.*;
import cn.com.nla.video.domain.bo.DeviceBo;
import cn.com.nla.video.domain.bo.VideoMediaServerBo;
import cn.com.nla.video.mapper.*;
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
class VideoDataContractTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final List<String> PRIVATE_FIELDS = List.of("password", "secret", "url", "srcUrl", "dstUrl");
    private JdbcDataSource dataSource;
    private SqlSession session;

    static Stream<Class<? extends BaseEntity>> entities() {
        return Stream.of(Device.class, DeviceChannel.class, DeviceAlarm.class, DeviceMobilePosition.class,
            VideoMediaServer.class, ParentPlatform.class, PlatformCatalog.class, PlatformGbStream.class,
            PlatformGbChannel.class, GbStream.class, StreamProxy.class, StreamPush.class);
    }

    static Stream<Class<? extends BaseEntity>> uniqueEntities() {
        return Stream.of(Device.class, DeviceChannel.class, VideoMediaServer.class, ParentPlatform.class,
            PlatformGbStream.class, PlatformGbChannel.class, GbStream.class, StreamProxy.class, StreamPush.class);
    }

    @BeforeEach
    void prepareDatabaseAndMappers() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:video_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_video.sql")) {
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
                    sql = sql.replaceAll("(?i)\\bkey (idx_\\w+)", "key " + table.group(1) + "_$1");
                    statement.execute(sql);
                }
            }
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("video-test", new JdbcTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig())
            .setMetaObjectHandler(new AuditFill()));
        for (Class<? extends BaseEntity> entity : entities().toList()) {
            configuration.addMapper(mapperClass(entity));
        }
        for (String name : List.of("PlatformGbChannel", "PlatformGbStream")) {
            String resource = "mapper/video/" + name + "Mapper.xml";
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
            .filter(f -> f.getType() == String.class && !f.getName().equals(info.getKeyProperty())
                && !f.getName().equals("delFlag")).findFirst().orElseThrow();
        changed.setAccessible(true); changed.set(update, "更新中文");
        assertEquals(1, mapper.updateById(update));
        BaseEntity afterUpdate = mapper.selectById(key);
        assertEquals("更新中文", changed.get(afterUpdate));
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
        Class<?> boType = Class.forName("cn.com.nla.video.domain.bo." + name + "Bo");
        Object bo = populated(boType);
        Object converted = convert("cn.com.nla.video.domain.bo." + name + "BoTo" + name + "Mapper", bo);
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
        Object view = convert("cn.com.nla.video.domain." + name + "To" + name + "VoMapper", entity);
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
            if (type == VideoMediaServer.class) { ((VideoMediaServer) entity).setId("ZLM-" + cycle); }
            mapper.insert(entity);
            BaseEntity duplicate = populated(type);
            if (type == VideoMediaServer.class) { ((VideoMediaServer) duplicate).setId("ZLM-duplicate-" + cycle); }
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
        for (var type : List.of(VideoMediaServer.class, PlatformCatalog.class)) {
            var mapper = mapper(type); var entity = populated(type); mapper.insert(entity);
            String key = (String) TableInfoHelper.getTableInfo(type).getPropertyValue(entity, "id");
            assertTrue(key.contains("中文")); mapper.deleteById(key);
            assertThrows(RuntimeException.class, () -> mapper.insert(populated(type)));
        }
    }

    @Test
    void channelAndStreamSharingUsesProtocolIdsAndBoundPlatformCatalogScope() {
        var fixture = sharing("平台' OR 1=1 --", "目录中文");
        sharing("其他平台", "其他目录");
        var channels = session.getMapper(PlatformGbChannelMapper.class)
            .selectSharedChannels(fixture.platform.getServerGbId(), List.of(fixture.catalog.getId()));
        assertThat(channels).hasSize(1); var channel = channels.getFirst();
        assertEquals(fixture.channel.getId(), channel.getId());
        assertEquals(fixture.channel.getChannelId(), channel.getChannelId());
        assertEquals(fixture.catalog.getId(), channel.getCatalogId());
        assertEquals(fixture.device.getHostAddress(), channel.getHostAddress());
        assertThat(JsonMapper.builder().build().writeValueAsString(channel)).doesNotContain("password");
        var streams = session.getMapper(PlatformGbStreamMapper.class)
            .selectSharedStreams(fixture.platform.getServerGbId(), List.of(fixture.catalog.getId()));
        assertThat(streams).hasSize(1);
        assertEquals(fixture.stream.getGbStreamId(), streams.getFirst().getGbStreamId());
        assertEquals(fixture.stream.getGbId(), fixture.streamLink.getGbStreamId());
        assertEquals(fixture.platform.getServerGbId(), streams.getFirst().getPlatformId());
        assertEquals(CREATED, streams.getFirst().getCreateTime());
        assertThat(session.getMapper(PlatformGbChannelMapper.class).selectSharedChannels("其他平台", List.of("目录中文"))).isEmpty();
        assertThat(session.getMapper(PlatformGbStreamMapper.class).selectSharedStreams("其他平台", List.of("目录中文"))).isEmpty();
    }

    @Test
    void nullEmptyUnknownScopesAndOrphanCatalogsReturnNoSharedRows() {
        var fixture = sharing("平台", "目录");
        var channels = session.getMapper(PlatformGbChannelMapper.class);
        var streams = session.getMapper(PlatformGbStreamMapper.class);
        for (List<String> ids : java.util.Arrays.asList(null, List.<String>of(), List.of("不存在"))) {
            assertThat(channels.selectSharedChannels("平台", ids)).isEmpty();
            assertThat(streams.selectSharedStreams("平台", ids)).isEmpty();
        }
        assertThat(channels.selectSharedChannels(null, List.of("目录"))).isEmpty();
        assertThat(streams.selectSharedStreams(null, List.of("目录"))).isEmpty();
        PlatformCatalog wrongPlatform = new PlatformCatalog(); wrongPlatform.setId(fixture.catalog.getId());
        wrongPlatform.setPlatformId("不匹配的平台"); session.getMapper(PlatformCatalogMapper.class).updateById(wrongPlatform);
        assertThat(channels.selectSharedChannels("平台", List.of("目录"))).isEmpty();
        assertThat(streams.selectSharedStreams("平台", List.of("目录"))).isEmpty();
    }

    static Stream<String> sharingParts() { return Stream.of("device", "channel", "channelLink", "stream", "streamLink", "catalog", "platform"); }

    @ParameterizedTest(name = "删除 {0} 后关联查询过滤对应记录")
    @MethodSource("sharingParts")
    void everyJoinedTableHonorsLogicalDeletion(String part) throws Exception {
        var fixture = sharing("平台", "目录");
        BaseEntity entity = (BaseEntity) fixture.getClass().getDeclaredField(part).get(fixture);
        var info = TableInfoHelper.getTableInfo(entity.getClass());
        mapper(entity.getClass()).deleteById((Serializable) info.getPropertyValue(entity, info.getKeyProperty()));
        var channels = session.getMapper(PlatformGbChannelMapper.class).selectSharedChannels("平台", List.of("目录"));
        var streams = session.getMapper(PlatformGbStreamMapper.class).selectSharedStreams("平台", List.of("目录"));
        assertThat(channels).hasSize(List.of("stream", "streamLink").contains(part) ? 1 : 0);
        assertThat(streams).hasSize(List.of("device", "channel", "channelLink").contains(part) ? 1 : 0);
    }

    @Test
    void databaseAndValidationRejectMissingKeysOversizedIdsAndInvalidPorts() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var bo = (VideoMediaServerBo) populated(VideoMediaServerBo.class);
            bo.setId("a".repeat(256)); bo.setHttpPort(65536); bo.setHookIp(" ");
            assertThat(validator.validate(bo, AddGroup.class)).extracting(v -> v.getPropertyPath().toString())
                .contains("id", "httpPort", "hookIp");
            var device = (DeviceBo) populated(DeviceBo.class); device.setHeartBeatInterval(-1);
            assertThat(validator.validate(device, AddGroup.class)).extracting(v -> v.getPropertyPath().toString())
                .contains("heartBeatInterval");
        }
        var media = (VideoMediaServer) populated(VideoMediaServer.class); media.setId(null);
        assertThrows(RuntimeException.class, () -> session.getMapper(VideoMediaServerMapper.class).insert(media));
        var device = (Device) populated(Device.class); device.setDeviceId("a".repeat(51));
        assertThrows(RuntimeException.class, () -> session.getMapper(DeviceMapper.class).insert(device));
        var alarm = (DeviceAlarm) populated(DeviceAlarm.class); alarm.setAlarmTime(null);
        assertThrows(RuntimeException.class, () -> session.getMapper(DeviceAlarmMapper.class).insert(alarm));
    }

    private Sharing sharing(String platformId, String catalogId) {
        var device = new Device(); device.setDeviceId("设备-" + platformId); device.setHostAddress("192.0.2.1:5060");
        device.setManufacturer("中文厂商"); device.setPassword("internal-device-secret"); session.getMapper(DeviceMapper.class).insert(device);
        var channel = new DeviceChannel(); channel.setDeviceId(device.getDeviceId()); channel.setChannelId("通道-" + platformId);
        channel.setPassword("internal-channel-secret"); session.getMapper(DeviceChannelMapper.class).insert(channel);
        var platform = new ParentPlatform(); platform.setServerGbId(platformId); platform.setDeviceGbId("本机编号");
        platform.setCatalogId(catalogId); session.getMapper(ParentPlatformMapper.class).insert(platform);
        var catalog = new PlatformCatalog(); catalog.setId(catalogId); catalog.setPlatformId(platformId); catalog.setName("目录名称");
        session.getMapper(PlatformCatalogMapper.class).insert(catalog);
        var channelLink = new PlatformGbChannel(); channelLink.setPlatformId(platformId); channelLink.setCatalogId(catalogId);
        channelLink.setDeviceChannelId(channel.getChannelId()); session.getMapper(PlatformGbChannelMapper.class).insert(channelLink);
        var stream = new GbStream(); stream.setApp("live"); stream.setStream("流-" + platformId); stream.setGbId("国标流-" + platformId);
        session.getMapper(GbStreamMapper.class).insert(stream);
        var streamLink = new PlatformGbStream(); streamLink.setPlatformId(platformId); streamLink.setCatalogId(catalogId);
        streamLink.setGbStreamId(stream.getGbId()); session.getMapper(PlatformGbStreamMapper.class).insert(streamLink);
        return new Sharing(device, channel, platform, catalog, channelLink, stream, streamLink);
    }

    @Test
    void globalChannelIdAndBothGbStreamBusinessKeysRemainUnique() {
        var channelMapper = session.getMapper(DeviceChannelMapper.class);
        var first = new DeviceChannel(); first.setDeviceId("设备一"); first.setChannelId("同一通道"); channelMapper.insert(first);
        var duplicate = new DeviceChannel(); duplicate.setDeviceId("设备二"); duplicate.setChannelId("同一通道");
        assertThrows(RuntimeException.class, () -> channelMapper.insert(duplicate));
        var streamMapper = session.getMapper(GbStreamMapper.class);
        var stream = new GbStream(); stream.setApp("live"); stream.setStream("one"); stream.setGbId("国标一"); streamMapper.insert(stream);
        var duplicateGb = new GbStream(); duplicateGb.setApp("other"); duplicateGb.setStream("two"); duplicateGb.setGbId("国标一");
        assertThrows(RuntimeException.class, () -> streamMapper.insert(duplicateGb));
        var duplicateAppStream = new GbStream(); duplicateAppStream.setApp("live"); duplicateAppStream.setStream("one"); duplicateAppStream.setGbId("国标二");
        assertThrows(RuntimeException.class, () -> streamMapper.insert(duplicateAppStream));
    }

    @Test
    void legacyDefaultsAndStringDeletionMarkerAreAppliedByDatabase() {
        var device = new Device(); device.setDeviceId("默认设备");
        var mapper = session.getMapper(DeviceMapper.class); mapper.insert(device);
        var loaded = mapper.selectById(device.getId());
        assertEquals(2, loaded.getOnline()); assertEquals(30, loaded.getHeartBeatInterval());
        assertEquals(215, loaded.getTreeType()); assertEquals(0, loaded.getSubscribeCycleForCatalog());
        assertEquals("0", loaded.getDelFlag());
        var media = new VideoMediaServer(); media.setId("协议节点"); media.setIp("192.0.2.1");
        media.setHookIp("192.0.2.2"); media.setSdpIp("192.0.2.1");
        var mediaMapper = session.getMapper(VideoMediaServerMapper.class); mediaMapper.insert(media);
        var mediaLoaded = mediaMapper.selectById(media.getId());
        assertEquals(0, mediaLoaded.getEnable()); assertEquals(1, mediaLoaded.getAutoConfig());
        assertEquals("协议节点", mediaLoaded.getId());
    }

    @Test
    void credentialInputsAcceptJsonButNeverEchoPasswordsOrRawStreamUrls() {
        var json = JsonMapper.builder().build();
        var device = json.readValue("{\"deviceId\":\"设备\",\"password\":\"test-only-password\"}", DeviceBo.class);
        assertEquals("test-only-password", device.getPassword());
        assertThat(json.writeValueAsString(device)).doesNotContain("password", "test-only-password");
        var proxy = json.readValue("{\"app\":\"live\",\"srcUrl\":\"rtsp://user:test-only-password@example.invalid/live\"}",
            cn.com.nla.video.domain.bo.StreamProxyBo.class);
        assertThat(proxy.getSrcUrl()).contains("test-only-password");
        assertThat(json.writeValueAsString(proxy)).doesNotContain("srcUrl", "test-only-password");
        assertThat(proxy.toString()).doesNotContain("test-only-password");
    }

    private record Sharing(Device device, DeviceChannel channel, ParentPlatform platform, PlatformCatalog catalog,
                           PlatformGbChannel channelLink, GbStream stream, PlatformGbStream streamLink) { }

    @SuppressWarnings("unchecked")
    private static Class<BaseMapper<BaseEntity>> mapperClass(Class<?> entity) throws ClassNotFoundException {
        return (Class<BaseMapper<BaseEntity>>) Class.forName("cn.com.nla.video.mapper." + entity.getSimpleName() + "Mapper");
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
            else if (field.getType() == Double.class) { field.set(object, 12.5); }
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
