package cn.com.nla.video;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.ParentPlatform;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.bo.*;
import cn.com.nla.video.mapper.*;
import cn.com.nla.video.service.*;
import cn.com.nla.video.service.impl.*;
import cn.com.nla.video.spi.DeviceChannelVoServiceImpl;
import cn.com.nla.video.spi.PlatformShareHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行交付 DDL 与真实 Mapper SQL 下的管理 Service 与协议 SPI 契约；不启动 SIP/ZLM/Redis 或 Spring 上下文。
 *
 * <p>覆盖阶段 6.5 第一轮的增量规则：业务键查重、编辑排除自身、删除前引用校验、级联共享清理、
 * 自定义 XML 查询，以及语音对讲占位返回。</p>
 */
class VideoServiceContractTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 12, 8, 0);
    private static final List<String> SECRET_FIELDS = List.of("password", "secret", "url", "srcUrl", "dstUrl");
    private JdbcDataSource dataSource;
    private SqlSession session;
    private DeviceMapper deviceMapper;
    private DeviceChannelMapper deviceChannelMapper;
    private PlatformCatalogMapper platformCatalogMapper;
    private PlatformGbChannelMapper platformGbChannelMapper;
    private PlatformGbStreamMapper platformGbStreamMapper;
    private ParentPlatformMapper parentPlatformMapper;
    private IDeviceService deviceService;
    private IDeviceChannelService deviceChannelService;
    private IPlatformCatalogService platformCatalogService;
    private IVideoMediaServerService mediaServerService;
    private IGbStreamService gbStreamService;
    private IParentPlatformService parentPlatformService;
    private IPlatformGbChannelService gbChannelService;
    private IPlatformGbStreamService gbStreamShareService;
    private IStreamProxyService streamProxyService;
    private IStreamPushService streamPushService;
    private PlatformShareHelper shareHelper;
    private DeviceChannelVoServiceImpl channelVoService;

    @BeforeAll
    static void installConverters() {
        VideoSpringSupport.ensureInstalled();
    }

    @BeforeEach
    void prepareDatabaseAndServices() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:video_service_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_video.sql")) {
            assertNotNull(input, "测试必须加载交付 DDL");
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        // H2 无 MySQL 表级存储选项与 STORED 关键字；菜单与角色授权种子属于 MySQL 交付脚本，此处只回放建表语句。
        ddl = ddl.replaceAll("(?im)^--[^\\r\\n]*", "")
            .replaceAll("(?i)\\) engine=innodb[^;]*;", ");")
            .replaceAll("(?i)\\bstored\\b", "");
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            int created = 0;
            for (String sql : ddl.split("(?m);\\s*$")) {
                if (sql.isBlank() || !sql.strip().regionMatches(true, 0, "create table", 0, 12)) { continue; }
                var table = java.util.regex.Pattern.compile("create table if not exists (\\w+)").matcher(sql);
                assertTrue(table.find());
                statement.execute(sql.replaceAll("(?i)\\bkey (idx_\\w+)", "key " + table.group(1) + "_$1"));
                created++;
            }
            assertEquals(12, created, "交付 DDL 应包含 12 张业务表");
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("video-service-test", new JdbcTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig())
            .setMetaObjectHandler(new AuditFill()));
        for (Class<?> mapper : List.of(DeviceMapper.class, DeviceChannelMapper.class, DeviceAlarmMapper.class,
            DeviceMobilePositionMapper.class, VideoMediaServerMapper.class, ParentPlatformMapper.class,
            PlatformCatalogMapper.class, PlatformGbStreamMapper.class, PlatformGbChannelMapper.class,
            GbStreamMapper.class, StreamProxyMapper.class, StreamPushMapper.class)) {
            configuration.addMapper(mapper);
        }
        for (String name : List.of("Device", "DeviceChannel", "GbStream", "ParentPlatform", "PlatformCatalog",
            "PlatformGbChannel", "PlatformGbStream", "StreamPush")) {
            String resource = "mapper/video/" + name + "Mapper.xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptor);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        deviceMapper = session.getMapper(DeviceMapper.class);
        deviceChannelMapper = session.getMapper(DeviceChannelMapper.class);
        platformCatalogMapper = session.getMapper(PlatformCatalogMapper.class);
        platformGbChannelMapper = session.getMapper(PlatformGbChannelMapper.class);
        platformGbStreamMapper = session.getMapper(PlatformGbStreamMapper.class);
        parentPlatformMapper = session.getMapper(ParentPlatformMapper.class);
        var gbStreamMapper = session.getMapper(GbStreamMapper.class);
        var mediaServerMapper = session.getMapper(VideoMediaServerMapper.class);
        var streamProxyMapper = session.getMapper(StreamProxyMapper.class);
        var streamPushMapper = session.getMapper(StreamPushMapper.class);
        deviceService = new DeviceServiceImpl(deviceMapper, deviceChannelMapper);
        deviceChannelService = new DeviceChannelServiceImpl(deviceChannelMapper, platformGbChannelMapper);
        platformCatalogService = new PlatformCatalogServiceImpl(platformCatalogMapper, parentPlatformMapper,
            platformGbChannelMapper, platformGbStreamMapper);
        mediaServerService = new VideoMediaServerServiceImpl(mediaServerMapper, deviceMapper, gbStreamMapper,
            streamProxyMapper, streamPushMapper);
        gbStreamService = new GbStreamServiceImpl(gbStreamMapper, platformGbStreamMapper);
        parentPlatformService = new ParentPlatformServiceImpl(parentPlatformMapper, platformCatalogMapper,
            platformGbChannelMapper, platformGbStreamMapper);
        gbChannelService = new PlatformGbChannelServiceImpl(platformGbChannelMapper);
        gbStreamShareService = new PlatformGbStreamServiceImpl(platformGbStreamMapper);
        streamProxyService = new StreamProxyServiceImpl(streamProxyMapper);
        streamPushService = new StreamPushServiceImpl(streamPushMapper);
        shareHelper = new PlatformShareHelper(deviceChannelMapper, gbStreamMapper, platformGbChannelMapper,
            platformGbStreamMapper);
        channelVoService = new DeviceChannelVoServiceImpl(deviceChannelMapper, shareHelper);
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
    void deviceDuplicateKeyRejectedAndDeleteBlockedByChannelReference() {
        var bo = deviceBo("设备一");
        assertTrue(deviceService.insertByBo(bo));
        assertTrue(assertThrows(ServiceException.class, () -> deviceService.insertByBo(deviceBo("设备一")))
            .getMessage().contains("设备国标编号已存在"));
        bo.setName("改名中文");
        assertTrue(deviceService.updateByBo(bo), "编辑查重应排除自身");
        assertEquals("改名中文", deviceService.queryByDeviceId("设备一").getName());
        assertTrue(deviceService.insertByBo(deviceBo("设备二")));
        var pageQuery = new PageQuery();
        pageQuery.setPageNum(1);
        pageQuery.setPageSize(1);
        var paged = deviceService.queryPageList(new DeviceBo(), pageQuery);
        assertEquals(1, paged.getRows().size());
        assertEquals(2, paged.getTotal());
        assertFalse(deviceService.deleteWithValidByIds(List.of(), true));
        assertFalse(deviceService.deleteWithValidByIds(List.of(9_000_000_000L), true));
        var channel = channelBo("设备一", "通道一");
        assertTrue(deviceChannelService.insertByBo(channel));
        assertTrue(assertThrows(ServiceException.class,
            () -> deviceService.deleteWithValidByIds(List.of(bo.getId()), true)).getMessage().contains("通道"));
        assertTrue(deviceChannelService.deleteWithValidByIds(List.of(channel.getId()), true));
        assertTrue(deviceService.deleteWithValidByIds(List.of(bo.getId()), true));
        assertNull(deviceService.queryByDeviceId("设备一"));
    }

    @Test
    void channelDeleteBlockedByPlatformCascadeUntilShareRowRemoved() {
        var bo = channelBo("设备一", "通道一");
        assertTrue(deviceChannelService.insertByBo(bo));
        assertTrue(assertThrows(ServiceException.class,
            () -> deviceChannelService.insertByBo(channelBo("设备一", "通道一"))).getMessage().contains("重复录入"));
        var rel = shareChannel("平台一", "目录一", "通道一");
        assertTrue(assertThrows(ServiceException.class,
            () -> deviceChannelService.deleteWithValidByIds(List.of(bo.getId()), true)).getMessage().contains("级联"));
        assertEquals(1, platformGbChannelMapper.deleteById(rel.getId()));
        assertEquals(1, deviceChannelService.deleteByDeviceId("设备一"));
        assertEquals(0L, deviceChannelMapper.selectCount(null));
        assertFalse(deviceChannelService.deleteWithValidByIds(List.of(), true));
    }

    @Test
    void catalogGuardsRejectBlankKeyDuplicateIdAndEveryReferenceKind() {
        var blank = populated(PlatformCatalogBo.class);
        blank.setId(" ");
        assertTrue(assertThrows(ServiceException.class, () -> platformCatalogService.insertByBo(blank))
            .getMessage().contains("不能为空"));
        assertTrue(assertThrows(ServiceException.class, () -> platformCatalogService.updateByBo(blank))
            .getMessage().contains("必须指定"));
        var root = catalog("目录根", "平台一");
        assertTrue(platformCatalogService.insertByBo(root));
        assertTrue(assertThrows(ServiceException.class,
            () -> platformCatalogService.insertByBo(catalog("目录根", "平台二"))).getMessage().contains("已存在"));
        var child = catalog("子目录", "平台一");
        child.setParentId(root.getId());
        assertTrue(platformCatalogService.insertByBo(child));
        assertEquals("平台一", platformCatalogService.queryById(root.getId()).getPlatformId());
        assertTrue(assertThrows(ServiceException.class,
            () -> platformCatalogService.deleteWithValidByIds(List.of(root.getId()), true))
            .getMessage().contains("子目录"));
        assertTrue(platformCatalogService.deleteWithValidByIds(List.of(child.getId()), true));
        var rel = shareChannel("平台一", root.getId(), "通道一");
        assertTrue(assertThrows(ServiceException.class,
            () -> platformCatalogService.deleteWithValidByIds(List.of(root.getId()), true)).getMessage().contains("通道"));
        assertEquals(1, platformGbChannelMapper.deleteById(rel.getId()));
        var platform = new ParentPlatform();
        platform.setServerGbId("平台一");
        platform.setDeviceGbId("本机编号");
        platform.setCatalogId(root.getId());
        assertEquals(1, parentPlatformMapper.insert(platform));
        assertTrue(assertThrows(ServiceException.class,
            () -> platformCatalogService.deleteWithValidByIds(List.of(root.getId()), true))
            .getMessage().contains("共享根目录"));
        assertFalse(platformCatalogService.deleteWithValidByIds(List.of("不存在目录"), true));
        assertEquals(1, platformCatalogService.queryByPlatformId("平台一").size());
        parentPlatformMapper.deleteById(platform.getId());
        assertTrue(platformCatalogService.deleteWithValidByIds(List.of(root.getId()), true));
    }

    @Test
    void mediaServerGuardsRejectBlankOrDuplicateKeySameIpPortAndBindings() {
        var blank = populated(VideoMediaServerBo.class);
        blank.setId(" ");
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.insertByBo(blank))
            .getMessage().contains("不能为空"));
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.updateByBo(blank))
            .getMessage().contains("必须指定"));
        var node = mediaServer("ZLM-1", "192.0.2.1", 8080);
        assertTrue(mediaServerService.insertByBo(node));
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.insertByBo(mediaServer("ZLM-1",
            "192.0.2.9", 9090))).getMessage().contains("已存在"));
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.insertByBo(mediaServer("ZLM-2",
            "192.0.2.1", 8080))).getMessage().contains("HTTP 端口"));
        assertTrue(mediaServerService.updateByBo(node), "编辑查重应排除自身编号");
        assertFalse(mediaServerService.deleteWithValidByIds(List.of(), true));
        var device = deviceBo("设备一");
        device.setMediaServerId("ZLM-1");
        assertTrue(deviceService.insertByBo(device));
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.deleteWithValidByIds(List.of("ZLM-1"), true))
            .getMessage().contains("设备"));
        assertTrue(deviceService.deleteWithValidByIds(List.of(device.getId()), true));
        var stream = gbStreamBo("live", "推流一", "国标一");
        stream.setMediaServerId("ZLM-1");
        assertTrue(gbStreamService.insertByBo(stream));
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.deleteWithValidByIds(List.of("ZLM-1"), true))
            .getMessage().contains("直播流"));
        assertTrue(gbStreamService.deleteWithValidByIds(List.of(stream.getGbStreamId()), true));
        var proxy = populated(StreamProxyBo.class);
        proxy.setApp("proxy");
        proxy.setStream("p1");
        proxy.setMediaServerId("ZLM-1");
        assertTrue(streamProxyService.insertByBo(proxy));
        assertTrue(assertThrows(ServiceException.class, () -> mediaServerService.deleteWithValidByIds(List.of("ZLM-1"), true))
            .getMessage().contains("拉流代理"));
        assertTrue(streamProxyService.deleteWithValidByIds(List.of(proxy.getId()), true));
        assertTrue(mediaServerService.deleteWithValidByIds(List.of("ZLM-1"), true));
        assertNull(mediaServerService.queryById("ZLM-1"));
    }

    @Test
    void streamAndShareRowsRejectDuplicateBusinessKeys() {
        var stream = gbStreamBo("live", "推流一", "国标一");
        assertTrue(gbStreamService.insertByBo(stream));
        assertTrue(assertThrows(ServiceException.class,
            () -> gbStreamService.insertByBo(gbStreamBo("live", "推流二", "国标一"))).getMessage().contains("已存在"));
        assertEquals("推流一", gbStreamService.queryByGbId("国标一").getStream());
        var streamShare = shareBo("平台一", "目录一", "国标一");
        assertTrue(gbStreamShareService.insertByBo(streamShare));
        assertTrue(assertThrows(ServiceException.class, () -> gbStreamShareService.insertByBo(shareBo("平台一",
            "目录一", "国标一"))).getMessage().contains("直播流"));
        assertTrue(assertThrows(ServiceException.class,
            () -> gbStreamService.deleteWithValidByIds(List.of(stream.getGbStreamId()), true)).getMessage().contains("级联"));
        assertEquals(1, platformGbStreamMapper.deleteById(streamShare.getId()));
        assertTrue(gbStreamService.deleteWithValidByIds(List.of(stream.getGbStreamId()), true));
        var channelShare = channelShareBo("平台一", "目录一", "通道一");
        assertTrue(gbChannelService.insertByBo(channelShare));
        assertTrue(assertThrows(ServiceException.class, () -> gbChannelService.insertByBo(channelShareBo("平台一",
            "目录一", "通道一"))).getMessage().contains("通道"));
        assertEquals(1, platformGbChannelMapper.deleteById(channelShare.getId()));
        assertTrue(streamProxyService.insertByBo(proxyOf("proxy", "p1")));
        assertTrue(assertThrows(ServiceException.class, () -> streamProxyService.insertByBo(proxyOf("proxy", "p1")))
            .getMessage().contains("拉流代理"));
        assertTrue(streamPushService.insertByBo(pushOf("push", "u1")));
        assertTrue(assertThrows(ServiceException.class, () -> streamPushService.insertByBo(pushOf("push", "u1")))
            .getMessage().contains("推流"));
        assertEquals("p1", streamProxyService.queryByAppAndStream("proxy", "p1").getStream());
        assertEquals("u1", streamPushService.queryByAppAndStream("push", "u1").getStream());
    }

    @Test
    void parentPlatformDeleteBlockedByCatalogAndShareRows() {
        var bo = populated(ParentPlatformBo.class);
        bo.setServerGbId("平台一");
        assertTrue(parentPlatformService.insertByBo(bo));
        var duplicate = populated(ParentPlatformBo.class);
        duplicate.setServerGbId("平台一");
        assertTrue(assertThrows(ServiceException.class, () -> parentPlatformService.insertByBo(duplicate))
            .getMessage().contains("已存在"));
        assertFalse(parentPlatformService.deleteWithValidByIds(List.of(), true));
        var catalog = catalog("目录一", "平台一");
        assertTrue(platformCatalogService.insertByBo(catalog));
        assertTrue(assertThrows(ServiceException.class,
            () -> parentPlatformService.deleteWithValidByIds(List.of(bo.getId()), true)).getMessage().contains("目录"));
        assertTrue(platformCatalogService.deleteWithValidByIds(List.of(catalog.getId()), true));
        var share = channelShareBo("平台一", "目录一", "通道一");
        assertTrue(gbChannelService.insertByBo(share));
        assertTrue(assertThrows(ServiceException.class,
            () -> parentPlatformService.deleteWithValidByIds(List.of(bo.getId()), true)).getMessage().contains("级联"));
        assertEquals(1, platformGbChannelMapper.deleteById(share.getId()));
        assertTrue(parentPlatformService.deleteWithValidByIds(List.of(bo.getId()), true));
        assertNull(parentPlatformService.queryById(bo.getId()));
    }

    @Test
    void spiChannelStatusAndStreamUpdatesStayWithinDatabase() {
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道一")));
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道二")));
        assertNull(channelVoService.findChannelId("不存在"));
        assertEquals("设备一", channelVoService.findChannelId("通道一").getDeviceId());
        assertNull(channelVoService.findDeviceIdChannelId("设备二", "通道一"));
        assertNotNull(channelVoService.findLastDevice("设备一"));
        assertEquals(2, channelVoService.queryAllChannels("设备一").size());
        channelVoService.deviceChannelOnline("设备一", "通道一", true);
        assertEquals(ConstEnum.Flag.YES.getValue(), status("通道一"));
        channelVoService.deviceChannelOnline("设备一", "", false);
        assertEquals(ConstEnum.Flag.NO.getValue(), status("通道一"), "通道编号为空时应更新该设备全部通道");
        assertEquals(ConstEnum.Flag.NO.getValue(), status("通道二"));
        channelVoService.startPlay("设备一", "通道一", "stream-1");
        assertEquals("stream-1", streamId("通道一"));
        channelVoService.stopPlay("设备一", "通道一");
        assertEquals("", streamId("通道一"));
        assertEquals(ConstEnum.Flag.NO.getValue(), channelVoService.del("", "通道一"));
        assertEquals(ConstEnum.Flag.NO.getValue(), channelVoService.delAll(""));
        assertEquals(ConstEnum.Flag.YES.getValue(), channelVoService.del("设备一", "通道一"));
        assertNull(channelVoService.findChannelId("通道一"));
        assertEquals(ConstEnum.Flag.YES.getValue(), channelVoService.delAll("设备一"));
        assertEquals(0L, deviceChannelMapper.selectCount(null));
    }

    @Test
    void spiAudioPushEndpointsStillReportDeferredBatch() {
        for (var result : List.of(channelVoService.findAudioPushPath("设备一", "通道一"),
            channelVoService.findAudioPushStatus("设备一", "通道一"),
            channelVoService.stopAudioPushStatus("设备一", "通道一"))) {
            assertEquals(RespCode.CODE_2.getValue(), result.getCode());
        }
    }

    @Test
    void spiShareQueriesReadCustomXmlAndSubCountRecountsChildren() {
        var parent = channelBo("设备一", "通道一");
        assertTrue(deviceChannelService.insertByBo(parent));
        var child = channelBo("设备一", "通道二");
        child.setParentId("通道一");
        assertTrue(deviceChannelService.insertByBo(child));
        var catalog = catalog("目录一", "平台一");
        assertTrue(platformCatalogService.insertByBo(catalog));
        var rel = shareChannel("平台一", catalog.getId(), "通道一");
        assertNull(channelVoService.findPlatformIdChannelId("平台二", "通道一"));
        assertEquals("通道一", channelVoService.findPlatformIdChannelId("平台一", "通道一").getChannelId());
        var shared = channelVoService.queryChannelWithCatalog("平台一");
        assertEquals(1, shared.size());
        assertEquals("设备一", shared.getFirst().getDeviceId());
        assertEquals(0, channelVoService.queryChannelWithCatalog("其他平台").size());
        // 目录与共享关系任一被逻辑删除后，级联目录查询不再下发该通道
        assertEquals(1, platformGbChannelMapper.deleteById(rel.getId()));
        assertEquals(0, channelVoService.queryChannelWithCatalog("平台一").size());
        deviceChannelMapper.updateChannelSubCount("设备一", "通道一");
        deviceChannelMapper.updateChannelSubCount("设备一", "通道二");
        assertEquals(1, subCount("通道一"));
        assertEquals(0, subCount("通道二"));
    }

    @Test
    void shareHelperCleanupRequiresBothKeysAndRemovesCascadeRows() {
        var gb = gbStreamBo("live", "推流一", "国标一");
        assertTrue(gbStreamService.insertByBo(gb));
        assertTrue(gbStreamShareService.insertByBo(shareBo("平台一", "目录一", "国标一")));
        shareHelper.removeStreamRel("", "推流一");
        assertEquals(1L, platformGbStreamMapper.selectCount(null));
        shareHelper.removeStreamRel("live", "推流一");
        assertEquals(0L, platformGbStreamMapper.selectCount(null));
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道一")));
        shareChannel("平台一", "目录一", "通道一");
        shareHelper.removeChannelRel(null, null);
        assertEquals(1L, platformGbChannelMapper.selectCount(null));
        shareHelper.removeChannelRel("设备一", null);
        assertEquals(0L, platformGbChannelMapper.selectCount(null));
    }

    private static DeviceBo deviceBo(String deviceId) {
        var bo = populated(DeviceBo.class);
        bo.setDeviceId(deviceId);
        return bo;
    }

    private static DeviceChannelBo channelBo(String deviceId, String channelId) {
        var bo = populated(DeviceChannelBo.class);
        bo.setDeviceId(deviceId);
        bo.setChannelId(channelId);
        return bo;
    }

    private static PlatformCatalogBo catalog(String id, String platformId) {
        var bo = populated(PlatformCatalogBo.class);
        bo.setId(id);
        bo.setPlatformId(platformId);
        return bo;
    }

    private static VideoMediaServerBo mediaServer(String id, String ip, int httpPort) {
        var bo = populated(VideoMediaServerBo.class);
        bo.setId(id);
        bo.setIp(ip);
        bo.setHttpPort(httpPort);
        return bo;
    }

    private static GbStreamBo gbStreamBo(String app, String stream, String gbId) {
        var bo = populated(GbStreamBo.class);
        bo.setApp(app);
        bo.setStream(stream);
        bo.setGbId(gbId);
        return bo;
    }

    private static StreamProxyBo proxyOf(String app, String stream) {
        var bo = populated(StreamProxyBo.class);
        bo.setApp(app);
        bo.setStream(stream);
        return bo;
    }

    private static StreamPushBo pushOf(String app, String stream) {
        var bo = populated(StreamPushBo.class);
        bo.setApp(app);
        bo.setStream(stream);
        return bo;
    }

    private static PlatformGbChannelBo channelShareBo(String platformId, String catalogId, String channelId) {
        var bo = populated(PlatformGbChannelBo.class);
        bo.setPlatformId(platformId);
        bo.setCatalogId(catalogId);
        bo.setDeviceChannelId(channelId);
        return bo;
    }

    private static PlatformGbStreamBo shareBo(String platformId, String catalogId, String gbId) {
        var bo = populated(PlatformGbStreamBo.class);
        bo.setPlatformId(platformId);
        bo.setCatalogId(catalogId);
        bo.setGbStreamId(gbId);
        return bo;
    }

    /** 共享关系走 Service 新增，顺带覆盖级联新增链路。 */
    private PlatformGbChannel shareChannel(String platformId, String catalogId, String channelId) {
        var bo = channelShareBo(platformId, catalogId, channelId);
        assertTrue(gbChannelService.insertByBo(bo));
        return platformGbChannelMapper.selectById(bo.getId());
    }

    private int status(String channelId) {
        return channel(channelId).getStatus();
    }

    private int subCount(String channelId) {
        return channel(channelId).getSubCount();
    }

    private String streamId(String channelId) {
        return channel(channelId).getStreamId();
    }

    private DeviceChannel channel(String channelId) {
        return deviceChannelMapper.lambda().eq(DeviceChannel::getChannelId, channelId).one();
    }

    /** 与交付 DDL 保持一致：字符串、整型、浮点、时间全部填充，凭据字段使用可识别的假值。 */
    private static <T> T populated(Class<T> type) {
        try {
            T object = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("delFlag")) { continue; }
                field.setAccessible(true);
                if (field.getType() == String.class) {
                    field.set(object, SECRET_FIELDS.contains(field.getName()) ? "private-" + field.getName()
                        : "中文-" + field.getName());
                } else if (field.getType() == Integer.class) {
                    field.set(object, 7);
                } else if (field.getType() == Double.class) {
                    field.set(object, 12.5);
                } else if (field.getType() == LocalDateTime.class) {
                    field.set(object, CREATED);
                }
            }
            return object;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("构造测试对象失败: " + type.getName(), e);
        }
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
