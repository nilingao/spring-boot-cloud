package cn.com.nla.callcenter.spi;

import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.callback.InviteErrorCallback;
import cn.com.nla.common.freeswitch.common.interfaces.ResultEvent;
import cn.com.nla.common.freeswitch.model.bean.UserModel;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;

/**
 * 协议侧坐席支撑服务。
 *
 * <p>迁移自旧 service.fs.AgentService：提供 FreeSWITCH XML 鉴权模型查询与坐席 SIP
 * 登录/注销/呼叫/挂断；旧 IService&lt;Agent&gt; 继承改为 mapper 直注（对齐 nla 分层惯例），
 * 旧 RestResult 响应包装统一替换为协议层 RestResultEvent。</p>
 *
 * @author TZY
 */
public interface AgentService {

    /**
     * 根据用户ID获取绑定的最新客服。
     *
     * @param userId 用户主键
     * @return 座席实体，未绑定返回 null
     */
    Agent findUserId(Long userId);

    /**
     * 根据 SIP 号获取 FreeSWITCH XML 鉴权模型。
     *
     * @param sip SIP 号码
     * @return 鉴权模型，未找到返回 null
     */
    UserModel findUserModel(String sip);

    /**
     * 挂断通话：通过 callId 向 FreeSWITCH 发送 SIP BYE。
     *
     * @param callId 通话编号
     * @return 处理结果事件
     */
    RestResultEvent stopStream(String callId);

    /**
     * 坐席登录：向 FreeSWITCH 发起 SIP REGISTER。
     *
     * @param agentKey 坐席账户
     * @param event    登录结果回调，可为 null
     */
    void login(String agentKey, ResultEvent event);

    /**
     * 坐席登出：向 FreeSWITCH 发起 SIP UNREGISTER。
     *
     * @param agentKey 坐席账户
     * @param event    登出结果回调，可为 null
     */
    void logout(String agentKey, ResultEvent event);

    /**
     * 纯 SIP 模式发起呼叫，不依赖 ZLM。
     *
     * @param typeName    通话类型名称
     * @param sipServer   SIP 服务
     * @param agentVoInfo 坐席信息（含 FS host/port）
     * @param caller      被叫号码
     * @param sdp         SIP SDP 内容（透传客户端 SDP 或业务层构建）
     * @param callBackId  回调 ID（非空 = 接听模式，空 = 主叫模式）
     * @param callback    呼叫结果回调
     */
    void callPhone(String typeName, SipServer sipServer, AgentVoInfo agentVoInfo,
                   String caller, String sdp, String callBackId,
                   InviteErrorCallback<Object> callback);
}
