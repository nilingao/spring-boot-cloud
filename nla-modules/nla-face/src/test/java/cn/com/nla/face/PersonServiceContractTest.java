package cn.com.nla.face;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.face.core.FaceFeatureCache;
import cn.com.nla.face.core.FaceImageDownloader;
import cn.com.nla.face.core.FacePipeline;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.domain.bo.PersonBo;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.mapper.PersonMapper;
import cn.com.nla.face.service.impl.PersonServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** H2 真实执行 CRUD 写路径，验证特征提取时机、extract 列保持与缓存键迁移，SDK 与下载全 mock。 */
class PersonServiceContractTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final String URL = "http://face.test/a.png";
    private static final String URL2 = "http://face.test/b.png";
    private JdbcDataSource dataSource;
    private SqlSession session;
    private PersonMapper mapper;
    private FacePipeline pipeline;
    private FaceImageDownloader downloader;
    private FaceFeatureCache featureCache;
    private PersonServiceImpl service;

    @BeforeAll
    static void installSpringHolder() {
        FaceSpringSupport.ensureInstalled();
    }

    @BeforeEach
    void prepareDatabaseAndService() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:face_svc_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_face.sql")) {
            assertNotNull(input, "交付 DDL 必须作为测试资源加载");
            ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        ddl = ddl.replaceFirst("(?is)\\) engine=innodb.*;\\s*$", ");");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(ddl);
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("face-svc-test", new JdbcTransactionFactory(), dataSource));
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
        pipeline = mock(FacePipeline.class);
        downloader = mock(FaceImageDownloader.class);
        featureCache = new FaceFeatureCache(mapper);
        service = new PersonServiceImpl(mapper, pipeline, downloader, featureCache);
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
    void insertRejectsDuplicateImgIdWithoutTouchingSdk() {
        mapper.insert(row("dup", URL, "[0.1,0.2]"));
        ServiceException e = assertThrows(ServiceException.class, () -> service.insertByBo(bo(null, "dup", URL)));
        assertThat(e.getMessage()).contains("图片编号已存在");
        verifyNoInteractions(downloader, pipeline);
    }

    @Test
    void insertExtractsStoresFeatureAndSeedsCache() {
        float[] feature = new float[]{0.5f, 0.25f};
        when(downloader.download(URL)).thenReturn(new byte[]{9});
        when(pipeline.extract(any(InputStream.class))).thenReturn(feature);
        PersonBo bo = bo(null, "新人员", URL);
        assertTrue(service.insertByBo(bo));
        assertNotNull(bo.getId());
        assertArrayEquals(feature, FaceFeatureCache.parseFeatures(storedExtract(bo.getId())));
        assertArrayEquals(feature, featureCache.snapshot().get("新人员"));
    }

    @Test
    void updateMissingRowIsRejected() {
        PersonBo bo = bo(9_999_999_999L, "不存在", URL);
        ServiceException e = assertThrows(ServiceException.class, () -> service.updateByBo(bo));
        assertThat(e.getMessage()).contains("人员信息不存在");
    }

    @Test
    void updateKeepsStoredExtractWhenUrlUnchanged() {
        Person existing = mapper.selectById(inserted("保持", URL, "[0.1,0.2]").getId());
        featureCache.put("保持", new float[]{0.1f, 0.2f});
        assertTrue(service.updateByBo(bo(existing.getId(), "保持", URL)));
        verifyNoInteractions(downloader, pipeline);
        assertEquals("[0.1,0.2]", storedExtract(existing.getId()));
        assertEquals("李四", mapper.selectById(existing.getId()).getPersonName());
        assertEquals(1, featureCache.size());
    }

    @Test
    void updateReextractsAndMigratesCacheWhenUrlAndImgIdChange() {
        Person existing = mapper.selectById(inserted("旧编号", URL, "[0.1,0.2]").getId());
        featureCache.put("旧编号", new float[]{0.1f, 0.2f});
        float[] fresh = new float[]{0.7f, 0.8f};
        when(downloader.download(URL2)).thenReturn(new byte[]{9});
        when(pipeline.extract(any(InputStream.class))).thenReturn(fresh);
        assertTrue(service.updateByBo(bo(existing.getId(), "新编号", URL2)));
        assertArrayEquals(fresh, FaceFeatureCache.parseFeatures(storedExtract(existing.getId())));
        assertArrayEquals(fresh, featureCache.snapshot().get("新编号"));
        assertFalse(featureCache.snapshot().containsKey("旧编号"));
    }

    @Test
    void updateImgIdOnlyMigratesStoredFeature() {
        Person existing = mapper.selectById(inserted("旧编号", URL, "[0.1,0.2]").getId());
        featureCache.put("旧编号", new float[]{0.1f, 0.2f});
        assertTrue(service.updateByBo(bo(existing.getId(), "新编号", URL)));
        verifyNoInteractions(downloader, pipeline);
        assertArrayEquals(new float[]{0.1f, 0.2f}, featureCache.snapshot().get("新编号"));
        assertFalse(featureCache.snapshot().containsKey("旧编号"));
    }

    @Test
    void updateFallsBackToReextractWhenStoredFeatureCorrupt() {
        Person existing = mapper.selectById(inserted("脏数据", URL, "not-json").getId());
        float[] fresh = new float[]{0.5f};
        when(downloader.download(anyString())).thenReturn(new byte[]{9});
        when(pipeline.extract(any(InputStream.class))).thenReturn(fresh);
        assertTrue(service.updateByBo(bo(existing.getId(), "新编号", URL)));
        verify(pipeline, times(1)).extract(any(InputStream.class));
        assertArrayEquals(fresh, featureCache.snapshot().get("新编号"));
    }

    @Test
    void deleteClearsCacheOnlyAfterRowsExist() {
        Person one = inserted("甲", URL, "[0.1]");
        Person two = inserted("乙", URL, "[0.2]");
        featureCache.put("甲", new float[]{0.1f});
        featureCache.put("乙", new float[]{0.2f});
        assertFalse(service.deleteWithValidByIds(List.of(), true));
        assertTrue(service.deleteWithValidByIds(List.of(one.getId(), two.getId()), true));
        assertEquals(0, featureCache.size());
        assertNull(mapper.selectById(one.getId()));
    }

    @Test
    void selectByImgIdsConvertsRowsAndSkipsEmptyRequest() {
        Person existing = inserted("检索", URL, "[0.1]");
        assertEquals(List.of(), service.selectByImgIds(List.of()));
        assertEquals(List.of(), service.selectByImgIds(null));
        List<PersonVo> found = service.selectByImgIds(List.of("检索", "缺失"));
        assertThat(found).extracting(PersonVo::getImgId).containsExactly("检索");
        assertEquals(existing.getPersonName(), found.getFirst().getPersonName());
    }

    private Person inserted(String imgId, String imgUrl, String extract) {
        Person person = row(imgId, imgUrl, extract);
        mapper.insert(person);
        return person;
    }

    private String storedExtract(Long id) {
        return mapper.selectFeatureList().stream().filter(p -> p.getId().equals(id))
            .findFirst().orElseThrow().getExtract();
    }

    private static Person row(String imgId, String imgUrl, String extract) {
        Person person = new Person();
        person.setImgId(imgId);
        person.setImgUrl(imgUrl);
        person.setExtract(extract);
        person.setPersonName("张三");
        person.setPersonAge(30);
        person.setGender(1);
        person.setAddress("上海");
        return person;
    }

    private static PersonBo bo(Long id, String imgId, String imgUrl) {
        PersonBo bo = new PersonBo();
        bo.setId(id);
        bo.setImgId(imgId);
        bo.setImgUrl(imgUrl);
        bo.setPersonName("李四");
        bo.setPersonAge(40);
        bo.setGender(2);
        bo.setAddress("北京");
        return bo;
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
