package cn.com.tzy.springbootfs.service.fs;

import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootentity.dome.fs.Agent;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.SipServer;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.callback.InviteErrorCallback;
import cn.com.tzy.springbootstarterfreeswitch.common.interfaces.ResultEvent;
import cn.com.tzy.springbootstarterfreeswitch.model.bean.UserModel;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AgentService extends IService<Agent> {

    /** 根据用户ID获取客服 */
    Agent findUserId(Long userId);

    /** 根据 SIP 号获取 FreeSWITCH XML 鉴权模型 */
    UserModel findUserModel(String sip);

    /**
     * 挂断通话：通过 callId 向 FreeSWITCH 发送 BYE
     */
    RestResult<?> stopStream(String callId);

    /** 坐席登录：向 FreeSWITCH 发起 SIP REGISTER */
    void login(String agentKey, ResultEvent event);

    /** 坐席登出：向 FreeSWITCH 发起 SIP UNREGISTER */
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

