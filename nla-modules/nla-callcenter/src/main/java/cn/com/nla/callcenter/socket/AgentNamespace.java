package cn.com.nla.callcenter.socket;

import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.callcenter.spi.AgentService;
import cn.com.nla.common.freeswitch.common.interfaces.ResultEvent;
import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.subscribe.notify.AgentLoginSubscribe;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import cn.dev33.satoken.stp.StpUtil;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.namespace.Namespace;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 坐席 Socket.IO 命名空间 {@code /socket_agent}。
 *
 * <p>坐席前端以 url 参数 {@code Authorization} 携带登录令牌连接，连接成功自动执行坐席 SIP 登录，
 * 断开时执行登出；同一坐席并发登录用 Redis 占位锁互斥，被占锁方挂到
 * {@link AgentLoginSubscribe} 延迟登录，锁持有方登出后发布登录事件唤醒等待方。
 * 旧工程的登录锁为 RedisUtils.getLock/releaseLock，nla 侧以 setObjectIfAbsent/deleteObject 等价替换。</p>
 *
 * <p>门控：Socket.IO 服务与 FreeSWITCH 协议栈都启用时才装配（监听器注册由
 * nla-common-socketio 的 SocketIoListenerRegistrar 在 socket-io.enabled 开启时统一完成）。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"socket-io.enabled", "freeswitch.enabled"}, havingValue = "true")
public class AgentNamespace implements NamespaceListener {

    /**
     * 登录占位锁的持有时长（秒），与旧工程一致
     */
    private static final long LOGIN_LOCK_SECONDS = 8L;

    private SocketIOServer socketIOServer;

    @Resource
    private AgentService agentService;

    @Override
    public String getNamespaceName() {
        return AgentCommon.SOCKET_AGENT;
    }

    @Override
    public void onConnect(SocketIOClient client) {
        log.info("客户端:{} 已连接", client.getSessionId());
        Agent agent = authenticateClient(client);
        if (agent == null) {
            return;
        }
        client.joinRoom(getSocketAgentKey(agent.getAgentKey()));
        RedisService.getAgentInfoManager().putAgentKey(client.getSessionId().toString(), agent.getAgentKey());
        handleAgentLogin(client, agent);
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        log.info("客户端:{} 断开连接", client.getSessionId());
        Agent agent = authenticateClient(client);
        if (agent == null) {
            return;
        }
        client.leaveRoom(getSocketAgentKey(agent.getAgentKey()));
        RedisService.getAgentInfoManager().delAgentKey(client.getSessionId().toString());
        handleAgentLogout(agent);
    }

    /**
     * 校验连接令牌并解析坐席：从 url 参数 Authorization 取 Sa-Token 令牌，
     * 兼容历史前端的 Bearer 前缀，loginId 形如 userType-userId，取尾段作为系统用户 ID。
     *
     * @param client 坐席连接
     * @return 命中的坐席，未命中返回 null
     */
    private Agent authenticateClient(SocketIOClient client) {
        Map<String, List<String>> urlParams = client.getHandshakeData().getUrlParams();
        List<String> authTokens = urlParams.get("Authorization");
        if (authTokens == null || authTokens.isEmpty()) {
            log.error("客户端:{} 未获取到坐席连接令牌", client.getSessionId());
            return null;
        }
        String token = authTokens.get(0);
        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            log.error("客户端:{} 令牌无效或已过期", client.getSessionId());
            return null;
        }
        Long userId = extractUserId(loginId.toString());
        if (userId == null) {
            log.error("客户端:{} 登录标识无法解析用户编号:{}", client.getSessionId(), loginId);
            return null;
        }
        Agent agent = agentService.findUserId(userId);
        if (agent == null) {
            log.error("客户端:{} 用户：{} 未获取到客服信息", client.getSessionId(), userId);
        }
        return agent;
    }

    /**
     * 从 Sa-Token loginId（userType-userId）解析用户编号。
     *
     * @param loginId 登录标识
     * @return 用户编号，格式非法返回 null
     */
    private Long extractUserId(String loginId) {
        int index = loginId.lastIndexOf('-');
        if (index < 0 || index == loginId.length() - 1) {
            return null;
        }
        try {
            return Long.valueOf(loginId.substring(index + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 坐席登录：抢到占位锁直接登录，否则挂延迟登录等待锁持有方登出唤醒。
     *
     * @param client 坐席连接
     * @param agent  坐席实体
     */
    private void handleAgentLogin(SocketIOClient client, Agent agent) {
        String key = AgentLoginSubscribe.FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER + agent.getAgentKey();
        if (RedisUtils.setObjectIfAbsent(key, agent.getAgentKey(), Duration.ofSeconds(LOGIN_LOCK_SECONDS))) {
            agentService.login(agent.getAgentKey(), sendLoginResult(client, key));
        } else {
            log.warn("已被加锁，订阅延迟登陆");
            RedisService.getAgentLoginSubscribe().addLoginSubscribe(agent.getAgentKey(),
                () -> agentService.login(agent.getAgentKey(), sendLoginResult(client, key)));
        }
    }

    /**
     * 组装登录结果回调：向坐席回发登录事件并释放占位锁。
     *
     * @param client 坐席连接
     * @param key    占位锁键
     * @return 登录结果回调
     */
    private ResultEvent sendLoginResult(SocketIOClient client, String key) {
        return data -> {
            client.sendEvent(AgentCommon.AGENT_OUT_LOGIN, data);
            RedisUtils.deleteObject(key);
        };
    }

    /**
     * 坐席登出：登出完成后发布登录事件，唤醒挂起在同一坐席上的延迟登录方。
     *
     * @param agent 坐席实体
     */
    private void handleAgentLogout(Agent agent) {
        String key = AgentLoginSubscribe.FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER + agent.getAgentKey();
        RedisUtils.setObjectIfAbsent(key, agent.getAgentKey(), Duration.ofSeconds(LOGIN_LOCK_SECONDS));
        agentService.logout(agent.getAgentKey(), data -> {
            RedisUtils.deleteObject(key);
            log.warn("退出完成，执行登陆操作");
            // 订阅端按 byte[] 接收，消息内容仅作唤醒信号
            RedisUtils.publish(key, SerializationUtils.serialize(agent.getAgentKey()));
            log.info("退出消息：{}", data.getMessage());
        });
    }

    @Override
    public SocketIOServer getSocketIOServer() {
        return socketIOServer;
    }

    @Override
    public void setSocketIOServer(SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
    }

    /**
     * 获取本命名空间对象。
     *
     * @return 命名空间
     */
    public Namespace getNamespace() {
        return (Namespace) socketIOServer.getNamespace(AgentCommon.SOCKET_AGENT);
    }

    /**
     * 生成坐席房间键。
     *
     * @param agentKey 坐席账户
     * @return 房间键
     */
    public String getSocketAgentKey(String agentKey) {
        return String.format("%s:%s", AgentCommon.SOCKET_AGENT, agentKey);
    }
}
