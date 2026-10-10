package cn.com.nla.system.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.exception.user.UserException;
import cn.com.nla.system.domain.vo.SysUserVo;
import cn.com.nla.system.mapper.SysSocialMapper;
import cn.com.nla.system.mapper.SysUserMapper;
import cn.com.nla.system.service.impl.SysSocialServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 执行交付 sys_social DDL 和真实 Mapper；系统用户和分布式锁由测试边界替代。 */
class SysXcxBindingTest {
    private static final String APP = "wx0123456789abcdef";
    private static final String OTHER_APP = "wxfedcba9876543210";
    private static final long OWNER = 9_007_199_254_740_993L;
    private JdbcDataSource database;
    private SqlSession session;
    private SysXcxBindingService service;
    private SysSocialMapper social;
    private SysUserMapper users;

    @BeforeEach
    void prepareDatabase() throws Exception {
        database = new JdbcDataSource();
        database.setURL("jdbc:h2:mem:xcx_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var input = getClass().getResourceAsStream("/nla_system.sql")) {
            assertNotNull(input); ddl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var table = Pattern.compile("(?is)create table sys_social\\s*\\((.*?)\\) engine[^;]*;").matcher(ddl);
        assertTrue(table.find());
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.execute("create table sys_social (" + table.group(1) + ")");
        }
        var configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("xcx-test", new JdbcTransactionFactory(), database));
        configuration.addMapper(SysSocialMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        social = session.getMapper(SysSocialMapper.class);
        users = mock(SysUserMapper.class);
        when(users.selectVoById(anyLong())).thenAnswer(call -> {
            var user = new SysUserVo(); user.setUserId(call.getArgument(0)); user.setUserName("contract-user");
            user.setNickName("Contract User"); user.setStatus("0"); return user;
        });
        service = new SysXcxBindingService(social, users);
    }

    @AfterEach
    void closeDatabase() throws Exception {
        if (session != null) { session.close(); }
        if (database != null) {
            try (var connection = database.getConnection(); var statement = connection.createStatement()) {
                statement.execute("shutdown");
            }
        }
    }

    @Test
    void bindStoresScopedIdentityAndDoesNotStoreSessionKey() {
        service.bind(OWNER, APP, "openid", "union-id");
        assertEquals(OWNER, service.findUserId(APP, "openid"));
        var bindings = social.selectList(null); assertEquals(1, bindings.size());
        var binding = bindings.getFirst();
        assertEquals(SysXcxBindingService.source(APP), binding.getSource());
        assertEquals(SysXcxBindingService.authId(APP, "openid"), binding.getAuthId());
        assertEquals("union-id", binding.getUnionId());
        assertEquals("", binding.getAccessToken()); assertNull(binding.getAccessCode()); assertNull(binding.getCode());
    }

    @Test
    void sameOpenidAcrossAppsCanBindDifferentAccounts() {
        service.bind(OWNER, APP, "same-openid", null);
        service.bind(OWNER + 1, OTHER_APP, "same-openid", null);
        assertEquals(OWNER, service.findUserId(APP, "same-openid"));
        assertEquals(OWNER + 1, service.findUserId(OTHER_APP, "same-openid"));
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "missing"));
    }

    @Test
    void identityOwnedByAnotherUserCannotBeReassigned() {
        service.bind(OWNER, APP, "openid", null);
        assertThrows(ServiceException.class, () -> service.bind(OWNER + 1, APP, "openid", null));
        assertEquals(OWNER, service.findUserId(APP, "openid")); assertEquals(1, social.selectList(null).size());
    }

    @Test
    void changingIdentityRequiresExplicitUnlink() {
        service.bind(OWNER, APP, "old-openid", null);
        assertThrows(ServiceException.class, () -> service.bind(OWNER, APP, "new-openid", null));
        assertEquals(OWNER, service.findUserId(APP, "old-openid"));
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "new-openid"));
    }

    @Test
    void repeatedBindingBySameOwnerIsIdempotent() {
        service.bind(OWNER, APP, "openid", null); service.bind(OWNER, APP, "openid", null);
        assertEquals(1, social.selectList(null).size()); assertEquals(OWNER, service.findUserId(APP, "openid"));
    }

    @Test
    void duplicateOrDamagedBindingsCannotBeUsedForLoginOrRebinding() throws Exception {
        service.bind(OWNER, APP, "openid", null);
        var binding = social.selectList(null).getFirst();
        try (var connection = database.getConnection(); var statement = connection.prepareStatement(
            "insert into sys_social(id,user_id,source,open_id,auth_id,user_name,access_token) values(?,?,?,?,?,'other','')")) {
            statement.setLong(1, binding.getId() + 1); statement.setLong(2, OWNER + 1);
            statement.setString(3, binding.getSource()); statement.setString(4, "openid"); statement.setString(5, binding.getAuthId());
            statement.executeUpdate();
        }
        session.clearCache(); // JDBC 故障注入绕过 Mapper，显式清理测试 SqlSession 的本地缓存。
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "openid"));
        assertThrows(ServiceException.class, () -> service.bind(OWNER, APP, "openid", null));
        assertEquals(2, social.selectList(null).size());
    }

    @Test
    void flaggedDeletedBindingCannotLoginAndDoesNotPreventFreshBinding() throws Exception {
        service.bind(OWNER, APP, "openid", null);
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("update sys_social set del_flag='1'");
        }
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "openid"));
        service.bind(OWNER + 1, APP, "openid", null);
        assertEquals(OWNER + 1, service.findUserId(APP, "openid"));
    }

    @Test
    void openidMatchingIsCaseSensitiveEvenWithInsensitiveDatabaseColumn() throws Exception {
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.execute("alter table sys_social alter column open_id varchar_ignorecase(255)");
        }
        service.bind(OWNER, APP, "CaseOpenid", null);
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "caseopenid"));
        service.bind(OWNER + 1, APP, "caseopenid", null);
        assertEquals(OWNER, service.findUserId(APP, "CaseOpenid"));
        assertEquals(OWNER + 1, service.findUserId(APP, "caseopenid"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled"})
    void unavailableSystemAccountCannotBind(String state) {
        if ("missing".equals(state)) { when(users.selectVoById(OWNER)).thenReturn(null); }
        else {
            var user = new SysUserVo(); user.setUserId(OWNER); user.setStatus("1");
            when(users.selectVoById(OWNER)).thenReturn(user);
        }
        assertThrows(UserException.class, () -> service.bind(OWNER, APP, "openid", null));
        assertTrue(social.selectList(null).isEmpty());
    }

    @Test
    void wrongAuthIdCannotResolveToAnAccount() throws Exception {
        service.bind(OWNER, APP, "openid", null);
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("update sys_social set auth_id='broken-auth-id'");
        }
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "openid"));
        assertThrows(ServiceException.class, () -> service.bind(OWNER, APP, "openid", null));
    }

    @Test
    void existingOwnerScopedUnlinkRemovesOnlyRequestedBinding() {
        service.bind(OWNER, APP, "openid", null); service.bind(OWNER, OTHER_APP, "openid", null);
        var binding = social.selectList(null).stream().filter(row -> row.getSource().equals(SysXcxBindingService.source(APP))).findFirst().orElseThrow();
        var unlink = new SysSocialServiceImpl(social);
        assertFalse(unlink.deleteByIdAndUserId(binding.getId(), OWNER + 1));
        assertTrue(unlink.deleteByIdAndUserId(binding.getId(), OWNER));
        assertThrows(ServiceException.class, () -> service.findUserId(APP, "openid"));
        assertEquals(OWNER, service.findUserId(OTHER_APP, "openid"));
    }
}
