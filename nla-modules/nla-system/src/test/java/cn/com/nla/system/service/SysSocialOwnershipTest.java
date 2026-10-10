package cn.com.nla.system.service;

import cn.com.nla.system.mapper.SysSocialMapper;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 使用交付 sys_social DDL 和真实 Mapper 验证解绑的用户归属条件。 */
class SysSocialOwnershipTest {
    private static final long OWNER = 9_007_199_254_740_993L;
    private JdbcDataSource dataSource;
    private SqlSession session;
    private SysSocialServiceImpl service;

    @BeforeEach
    void prepareDatabase() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:social_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        String ddl;
        try (var resource = getClass().getResourceAsStream("/nla_system.sql")) {
            assertNotNull(resource); ddl = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        var table = Pattern.compile("(?is)create table sys_social\\s*\\((.*?)\\) engine[^;]*;").matcher(ddl);
        assertTrue(table.find(), "必须使用交付 sys_social 表，不能执行初始化种子或 DROP");
        try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute("create table sys_social (" + table.group(1) + ")");
        }
        var configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("ownership-test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(SysSocialMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        service = new SysSocialServiceImpl(session.getMapper(SysSocialMapper.class));
        insert(101L, OWNER); insert(102L, OWNER + 1);
    }

    @AfterEach
    void closeDatabase() throws Exception {
        if (session != null) { session.close(); }
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("drop all objects"); statement.execute("shutdown");
        }
    }

    @Test
    void ownBindingCanBeRemovedWithoutDeletingOtherOwners() throws Exception {
        assertTrue(service.deleteByIdAndUserId(101L, OWNER));
        assertFalse(exists(101L)); assertTrue(exists(102L));
        assertFalse(service.deleteByIdAndUserId(101L, OWNER));
    }

    @Test
    void otherUsersBindingCannotBeRemoved() throws Exception {
        assertFalse(service.deleteByIdAndUserId(102L, OWNER));
        assertTrue(exists(101L)); assertTrue(exists(102L));
    }

    @Test
    void missingOwnerOrBindingNeverExpandsDeleteScope() throws Exception {
        assertFalse(service.deleteByIdAndUserId(101L, null));
        assertFalse(service.deleteByIdAndUserId(null, OWNER));
        assertFalse(service.deleteByIdAndUserId(999L, OWNER));
        assertTrue(exists(101L)); assertTrue(exists(102L));
    }

    @Test
    void ownerIsCheckedAtDeletionEvenAfterBindingWasReassigned() throws Exception {
        try (var connection = dataSource.getConnection(); var sql = connection.prepareStatement("update sys_social set user_id = ? where id = ?")) {
            sql.setLong(1, OWNER + 1); sql.setLong(2, 101L); assertEquals(1, sql.executeUpdate());
        }
        assertFalse(service.deleteByIdAndUserId(101L, OWNER)); assertTrue(exists(101L));
        assertTrue(service.deleteByIdAndUserId(101L, OWNER + 1)); assertTrue(exists(102L));
    }

    private void insert(long id, long owner) throws Exception {
        try (var connection = dataSource.getConnection(); var sql = connection.prepareStatement(
            "insert into sys_social(id,user_id,auth_id,source,user_name,access_token) values (?,?,?,'test','contract-user','test-only-token')")) {
            sql.setLong(1, id); sql.setLong(2, owner); sql.setString(3, "test-" + id); sql.executeUpdate();
        }
    }

    private boolean exists(long id) throws Exception {
        try (var connection = dataSource.getConnection(); var sql = connection.prepareStatement("select count(*) from sys_social where id = ?")) {
            sql.setLong(1, id);
            try (var rows = sql.executeQuery()) { assertTrue(rows.next()); return rows.getInt(1) == 1; }
        }
    }
}
