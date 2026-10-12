package cn.com.nla.video;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedResult;
import cn.com.nla.common.video.basic.vo.media.ZLMServerConfig;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.basic.vo.video.StreamProxyVo;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.redis.impl.InviteStreamManager;
import cn.com.nla.common.video.core.redis.impl.RegisterServerManager;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.service.video.MediaServerVoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.service.video.PlatformCatalogVoService;
import cn.com.nla.common.video.core.service.video.StreamProxyVoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.video.domain.DeviceChannel;
import cn.com.nla.video.domain.ParentPlatform;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.StreamProxy;
import cn.com.nla.video.domain.StreamPush;
import cn.com.nla.video.domain.bo.*;
import cn.com.nla.video.mapper.*;
import cn.com.nla.video.service.*;
import cn.com.nla.video.service.impl.*;
import cn.com.nla.video.spi.DeviceChannelVoServiceImpl;
import cn.com.nla.video.spi.PlatformShareHelper;
import cn.hutool.core.lang.tree.Tree;
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
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private GbStreamMapper gbStreamMapper;
    private StreamProxyMapper streamProxyMapper;
    private StreamPushMapper streamPushMapper;
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
    private VideoStreamServiceImpl streamRuntimeService;
    private VideoPlatformShareServiceImpl platformShareService;
    private IVideoChannelCatalogService channelCatalogService;

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
        gbStreamMapper = session.getMapper(GbStreamMapper.class);
        var mediaServerMapper = session.getMapper(VideoMediaServerMapper.class);
        streamProxyMapper = session.getMapper(StreamProxyMapper.class);
        streamPushMapper = session.getMapper(StreamPushMapper.class);
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
        channelVoService = new DeviceChannelVoServiceImpl(deviceChannelMapper, shareHelper,
            Mockito.mock(DynamicTask.class), Mockito.mock(SipServer.class), Mockito.mock(SIPCommander.class));
        channelCatalogService = Mockito.mock(IVideoChannelCatalogService.class);
        streamRuntimeService = new VideoStreamServiceImpl(streamProxyMapper, streamPushMapper);
        platformShareService = new VideoPlatformShareServiceImpl(deviceMapper, deviceChannelMapper,
            gbStreamMapper, platformCatalogMapper, platformGbChannelMapper, platformGbStreamMapper,
            gbStreamService, channelCatalogService);
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
    void spiAudioPushEndpointsFailFastWithoutProtocolStack() {
        // 语音对讲在 SIP/ZLM/Redis 都缺席时按协议语义快速失败，不触碰守护任务与 BYE 指令
        Mockito.reset(VideoSpringSupport.mock(SsrcTransactionManager.class),
            VideoSpringSupport.mock(DeviceChannelVoService.class),
            VideoSpringSupport.mock(DeviceVoService.class));
        var path = channelVoService.findAudioPushPath("设备一", "通道一");
        assertEquals(RespCode.CODE_2.getValue(), path.getCode());
        assertEquals("未获取设备通道信息", path.getMessage());
        var status = channelVoService.findAudioPushStatus("设备一", "通道一");
        assertEquals(RespCode.CODE_0.getValue(), status.getCode());
        assertEquals(Boolean.FALSE, status.getData());
        var stop = channelVoService.stopAudioPushStatus("设备一", "通道一");
        assertEquals(RespCode.CODE_2.getValue(), stop.getCode());
        assertEquals("未获取设备信息", stop.getMessage());
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

    /**
     * 级联目录通道查询必须用 WGS84 坐标与共享目录编号覆盖通道自身的原始坐标与父级编号；
     * 与 {@code ch.*} 产生的同名列遮蔽会让上级平台拿到 GCJ-02 坐标并挂错父目录。
     */
    @Test
    void channelCatalogQueryOverridesCoordinatesAndParentIdWithoutLabelShadowing() {
        var bo = channelBo("设备一", "通道一");
        bo.setParentId("通道二");
        bo.setLongitude(116.401);
        bo.setLatitude(39.901);
        bo.setLongitudeWgs84(116.403);
        bo.setLatitudeWgs84(39.903);
        assertTrue(deviceChannelService.insertByBo(bo));
        var catalog = catalog("目录一", "平台一");
        assertTrue(platformCatalogService.insertByBo(catalog));
        shareChannel("平台一", catalog.getId(), "通道一");
        var shared = channelVoService.queryChannelWithCatalog("平台一");
        assertEquals(1, shared.size());
        var channel = shared.getFirst();
        assertEquals("目录一", channel.getParentId(), "父级编号应取共享所在目录");
        assertNotNull(channel.getLongitude());
        assertNotNull(channel.getLatitude());
        assertEquals(116.403, channel.getLongitude(), 1e-9);
        assertEquals(39.903, channel.getLatitude(), 1e-9);
        assertEquals(116.403, channel.getLongitudeWgs84(), 1e-9);
        assertEquals(0, channel.getChannelType().intValue(), "级联下发的国标通道类型固定为 0");
    }

    /**
     * 流媒体运行态端点只借道静态持有器与 MediaClient，不落库；节点缺失、流未上线与协议失败都快速中断。
     */
    @Test
    void streamRuntimeQueriesResolveStaticHoldersAndFailFast() {
        var inviteStreamManager = VideoSpringSupport.mock(InviteStreamManager.class);
        var mediaServerVoService = VideoSpringSupport.mock(MediaServerVoService.class);
        var streamProxyVoService = VideoSpringSupport.mock(StreamProxyVoService.class);
        Mockito.reset(inviteStreamManager, mediaServerVoService, streamProxyVoService);

        StreamInfo playing = new StreamInfo();
        InviteInfo inviteInfo = new InviteInfo();
        inviteInfo.setStreamInfo(playing);
        Mockito.when(inviteStreamManager.getInviteInfo(null, "设备一", "通道一", null, null)).thenReturn(inviteInfo);
        assertSame(playing, streamRuntimeService.queryInviteStreamUrl("设备一", "通道一"));
        assertTrue(assertThrows(ServiceException.class,
            () -> streamRuntimeService.queryInviteStreamUrl("设备二", "通道一")).getMessage().contains("当前流未播放"));

        MediaServerVo node = new MediaServerVo();
        Mockito.when(mediaServerVoService.findOnLineMediaServerId("ZLM-1")).thenReturn(node);
        try (var mediaClient = Mockito.mockStatic(MediaClient.class)) {
            var offline = new OnStreamChangedResult();
            offline.setCode(RespCode.CODE_2.getValue());
            mediaClient.when(() -> MediaClient.getMediaInfo(node, null, null, "live", "s1")).thenReturn(offline);
            assertTrue(assertThrows(ServiceException.class,
                () -> streamRuntimeService.queryMediaInfo("live", "s1", "ZLM-9")).getMessage().contains("未获取流媒体信息"));
            assertTrue(assertThrows(ServiceException.class,
                () -> streamRuntimeService.queryMediaInfo("live", "s1", "ZLM-1")).getMessage().contains("未获取当前流信息"));
            var online = new OnStreamChangedResult();
            online.setCode(RespCode.CODE_0.getValue());
            mediaClient.when(() -> MediaClient.getMediaInfo(node, null, null, "live", "ok")).thenReturn(online);
            assertSame(online, streamRuntimeService.queryMediaInfo("live", "ok", "ZLM-1"));
            // FFmpeg 模板：节点与配置任一缺失都中断
            assertTrue(assertThrows(ServiceException.class, () -> streamRuntimeService.queryFfmpegCmd("ZLM-9"))
                .getMessage().contains("未获取到当前流媒体信息"));
            var config = new ZLMServerConfig();
            config.setFfmpegCmd("ffmpeg %s");
            mediaClient.when(() -> MediaClient.getZLMServerConfig(node)).thenReturn(config);
            assertEquals("ffmpeg %s", streamRuntimeService.queryFfmpegCmd("ZLM-1"));
        }

        var proxy = proxyOf("proxy", "p1");
        assertTrue(streamProxyService.insertByBo(proxy));
        assertTrue(assertThrows(ServiceException.class, () -> streamRuntimeService.queryProxyStreamUrl(proxy.getId()))
            .getMessage().contains("当前流未上线"));
        streamProxyMapper.lambda()
            .set(StreamProxy::getStatus, ConstEnum.Flag.YES.getValue())
            .set(StreamProxy::getMediaServerId, "ZLM-1")
            .eq(StreamProxy::getId, proxy.getId())
            .update();
        try (var mediaClient = Mockito.mockStatic(MediaClient.class)) {
            mediaClient.when(() -> MediaClient.getStreamInfo(node, "proxy", "p1", null, null)).thenReturn(null);
            assertTrue(assertThrows(ServiceException.class, () -> streamRuntimeService.queryProxyStreamUrl(proxy.getId()))
                .getMessage().contains("未获取当前流信息"));
            StreamInfo proxyInfo = new StreamInfo();
            mediaClient.when(() -> MediaClient.getStreamInfo(node, "proxy", "p1", null, null)).thenReturn(proxyInfo);
            assertSame(proxyInfo, streamRuntimeService.queryProxyStreamUrl(proxy.getId()));
        }
        Mockito.when(streamProxyVoService.start(Mockito.any(StreamProxyVo.class))).thenReturn(true);
        streamRuntimeService.startProxy(proxy.getId());
        Mockito.when(streamProxyVoService.stop("proxy", "p1")).thenReturn(true);
        streamRuntimeService.stopProxy(proxy.getId());
        Mockito.when(streamProxyVoService.stop("proxy", "p1")).thenReturn(false);
        assertTrue(assertThrows(ServiceException.class, () -> streamRuntimeService.stopProxy(proxy.getId()))
            .getMessage().contains("停用失败"));
        assertTrue(assertThrows(ServiceException.class, () -> streamRuntimeService.startProxy(9_000_000_000L))
            .getMessage().contains("未获取拉流代理"));

        var push = pushOf("push", "u1");
        assertTrue(streamPushService.insertByBo(push));
        assertTrue(assertThrows(ServiceException.class, () -> streamRuntimeService.queryPushStreamUrl(push.getId()))
            .getMessage().contains("当前流未上线"));
    }

    /**
     * 上级平台级联共享通道：可见性收敛到普通设备、目录事件按平台推送、已绑定去重、回显键含所属设备编号。
     */
    @Test
    void platformShareChannelBindingFollowsVisibilityAndCatalogEvents() {
        var parentPlatformVoService = VideoSpringSupport.mock(ParentPlatformVoService.class);
        var platformCatalogVoService = VideoSpringSupport.mock(PlatformCatalogVoService.class);
        var registerServerManager = VideoSpringSupport.mock(RegisterServerManager.class);
        Mockito.reset(parentPlatformVoService, platformCatalogVoService, registerServerManager,
            channelCatalogService);
        List<String> catalogEvents = new CopyOnWriteArrayList<>();
        Mockito.doAnswer(invocation -> {
            Object channels = invocation.getArgument(2);
            Object streams = invocation.getArgument(3);
            catalogEvents.add(invocation.getArgument(0) + "|" + invocation.getArgument(1)
                + "|ch=" + (channels == null ? 0 : ((List<?>) channels).size())
                + "|st=" + (streams == null ? 0 : ((List<?>) streams).size()));
            return null;
        }).when(platformCatalogVoService).handleCatalogEvent(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any());
        ParentPlatformVo platform = new ParentPlatformVo();
        platform.setServerGbId("平台一");
        platform.setName("平台名称");
        Mockito.when(parentPlatformVoService.getParentPlatformByServerGbId("平台一")).thenReturn(platform);
        assertTrue(assertThrows(ServiceException.class, () -> platformShareService.queryCatalogTree("平台九"))
            .getMessage().contains("未获取上级平台信息"));

        var device = deviceBo("设备一");
        device.setHasAdministrator(0);
        assertTrue(deviceService.insertByBo(device));
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道一")));
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道二")));
        var catalog = catalog("目录一", "平台一");
        catalog.setParentId("平台一");
        assertTrue(platformCatalogService.insertByBo(catalog));
        var child = catalog("子目录", "平台一");
        child.setParentId("目录一");
        assertTrue(platformCatalogService.insertByBo(child));
        // 级联共享查询联表校验上级平台行，必须真实落库
        var platformRow = new ParentPlatform();
        platformRow.setServerGbId("平台一");
        platformRow.setDeviceGbId("本机编号");
        platformRow.setCatalogId(catalog.getId());
        assertEquals(1, parentPlatformMapper.insert(platformRow));

        var sipList = List.of(Address.builder().gbId("平台一").ip("192.0.2.5").port(5060).build());
        Mockito.when(registerServerManager.getSipList()).thenReturn(sipList);
        assertSame(sipList, platformShareService.querySipList());
        var channelTree = List.of(new Tree<String>());
        Mockito.when(channelCatalogService.queryTree()).thenReturn(channelTree);
        assertSame(channelTree, platformShareService.queryChannelTree());
        var tree = platformShareService.queryCatalogTree("平台一");
        assertEquals(1, tree.size());
        assertEquals("平台一", tree.getFirst().getId());
        assertEquals("平台名称", tree.getFirst().getName());
        assertEquals(1, tree.getFirst().getChildren().size());
        assertEquals("目录一", tree.getFirst().getChildren().getFirst().getId());

        var bo = sharePlatformBo("平台一", "目录一");
        bo.setGbIdList(List.of("通道一", "通道二"));
        platformShareService.bindChannel(bo);
        assertEquals(2L, platformGbChannelMapper.selectCount(null));
        assertEquals(CatalogEventConstant.ADD + "|平台一|ch=2|st=0", catalogEvents.getFirst());
        // 已绑定的通道重复共享会被拒绝，不产生第二份关系；
        // insertBatch 走独立批处理会话提交，需先清一级缓存否则查重命中空结果旧缓存
        session.clearCache();
        var rebind = sharePlatformBo("平台一", "子目录");
        rebind.setGbIdList(List.of("通道一"));
        assertTrue(assertThrows(ServiceException.class, () -> platformShareService.bindChannel(rebind))
            .getMessage().contains("未获取添加通道信息"));
        // 回显键包含通道与所属设备编号，目录范围外的绑定不计入 useCatalogGbIdList
        var keys = platformShareService.queryChannelBindKey(sharePlatformBo("平台一", "子目录"));
        assertEquals(3, keys.getAllGbIdList().size());
        assertTrue(keys.getAllGbIdList().containsAll(List.of("通道一", "通道二", "设备一")));
        assertTrue(keys.getUseCatalogGbIdList().isEmpty());
        assertEquals(3, platformShareService.queryChannelBindKey(sharePlatformBo("平台一", "目录一"))
            .getUseCatalogGbIdList().size());
        var withSub = sharePlatformBo("平台一", "目录一");
        withSub.setIsSub(ConstEnum.Flag.YES.getValue());
        assertEquals(3, platformShareService.queryChannelBindKey(withSub).getUseCatalogGbIdList().size(),
            "包含子目录时目录范围取并集");
        // 非全量操作必须携带编号集合
        assertTrue(assertThrows(ServiceException.class,
            () -> platformShareService.bindChannel(sharePlatformBo("平台一", "目录一")))
            .getMessage().contains("请选择需要操作的数据"));

        var unbind = sharePlatformBo("平台一", "目录一");
        unbind.setGbIdList(List.of("通道一"));
        platformShareService.unbindChannel(unbind);
        assertEquals(1L, platformGbChannelMapper.selectCount(null));
        assertEquals(CatalogEventConstant.DEL + "|平台一|ch=1|st=0", catalogEvents.get(1));
        var missing = sharePlatformBo("平台一", "子目录");
        missing.setIsAll(ConstEnum.Flag.YES.getValue());
        assertTrue(assertThrows(ServiceException.class, () -> platformShareService.unbindChannel(missing))
            .getMessage().contains("未获取移除通道信息"));
    }

    /**
     * 国标流共享与目录关系解除：流绑定走同一事件通道，解除关系按类型委派且对空绑定宽容。
     */
    @Test
    void platformShareStreamBindingAndRelationRemovalDelegateByType() {
        var parentPlatformVoService = VideoSpringSupport.mock(ParentPlatformVoService.class);
        var platformCatalogVoService = VideoSpringSupport.mock(PlatformCatalogVoService.class);
        Mockito.reset(parentPlatformVoService, platformCatalogVoService);
        List<String> catalogEvents = new CopyOnWriteArrayList<>();
        Mockito.doAnswer(invocation -> {
            Object channels = invocation.getArgument(2);
            Object streams = invocation.getArgument(3);
            catalogEvents.add(invocation.getArgument(0) + "|" + invocation.getArgument(1)
                + "|ch=" + (channels == null ? 0 : ((List<?>) channels).size())
                + "|st=" + (streams == null ? 0 : ((List<?>) streams).size()));
            return null;
        }).when(platformCatalogVoService).handleCatalogEvent(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any());
        ParentPlatformVo platform = new ParentPlatformVo();
        platform.setServerGbId("平台一");
        platform.setName("平台名称");
        Mockito.when(parentPlatformVoService.getParentPlatformByServerGbId("平台一")).thenReturn(platform);

        var device = deviceBo("设备一");
        device.setHasAdministrator(0);
        assertTrue(deviceService.insertByBo(device));
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道一")));
        assertTrue(deviceChannelService.insertByBo(channelBo("设备一", "通道二")));
        var catalog = catalog("目录一", "平台一");
        catalog.setParentId("平台一");
        assertTrue(platformCatalogService.insertByBo(catalog));
        // 级联共享查询联表校验上级平台行，必须真实落库
        var platformRow = new ParentPlatform();
        platformRow.setServerGbId("平台一");
        platformRow.setDeviceGbId("本机编号");
        platformRow.setCatalogId(catalog.getId());
        assertEquals(1, parentPlatformMapper.insert(platformRow));
        assertTrue(gbStreamService.insertByBo(gbStreamBo("live", "推流一", "国标一")));
        assertEquals(1, platformShareService.queryStreamList().size());

        var streamBo = sharePlatformBo("平台一", "目录一");
        streamBo.setGbIdList(List.of("国标一"));
        platformShareService.bindStream(streamBo);
        assertEquals(1L, platformGbStreamMapper.selectCount(null));
        assertEquals(CatalogEventConstant.ADD + "|平台一|ch=0|st=1", catalogEvents.getFirst());
        var streamKeys = platformShareService.queryStreamBindKey(sharePlatformBo("平台一", "目录一"));
        assertEquals(List.of("国标一"), streamKeys.getUseCatalogGbIdList());
        assertTrue(streamKeys.getAllGbIdList().contains("国标一"));

        var channels = sharePlatformBo("平台一", "目录一");
        channels.setGbIdList(List.of("通道一", "通道二"));
        platformShareService.bindChannel(channels);
        // 解除类型只对流生效时通道关系保留
        platformShareService.deleteCatalogRelation("目录一", 2);
        assertEquals(0L, platformGbStreamMapper.selectCount(null));
        assertEquals(2L, platformGbChannelMapper.selectCount(null));
        assertEquals(CatalogEventConstant.DEL + "|平台一|ch=0|st=1", catalogEvents.get(2));
        // 解除全部关系对空绑定宽容，不会因一侧没有数据而中断；先清一级缓存避免命中解除前的旧查询结果
        session.clearCache();
        platformShareService.deleteCatalogRelation("目录一", null);
        assertEquals(0L, platformGbChannelMapper.selectCount(null));
        assertEquals(CatalogEventConstant.DEL + "|平台一|ch=2|st=0", catalogEvents.get(3));
        assertEquals(4, catalogEvents.size(), "空绑定解除不推送目录事件");
        var missing = sharePlatformBo("平台一", "目录一");
        missing.setIsAll(ConstEnum.Flag.YES.getValue());
        assertTrue(assertThrows(ServiceException.class, () -> platformShareService.unbindStream(missing))
            .getMessage().contains("未获取移除国标流信息"));
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

    private static VideoPlatformShareBo sharePlatformBo(String platformId, String catalogId) {
        var bo = new VideoPlatformShareBo();
        bo.setPlatformId(platformId);
        bo.setCatalogId(catalogId);
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
