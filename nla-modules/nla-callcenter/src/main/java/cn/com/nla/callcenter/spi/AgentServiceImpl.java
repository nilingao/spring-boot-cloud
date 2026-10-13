package cn.com.nla.callcenter.spi;

import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.callcenter.enums.InviteErrorCode;
import cn.com.nla.callcenter.mapper.AgentMapper;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.callback.InviteErrorCallback;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.freeswitch.common.interfaces.ResultEvent;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.model.bean.UserModel;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.service.SipService;
import cn.com.nla.common.freeswitch.service.freeswitch.AgentVoService;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.vo.sip.SsrcTransaction;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 协议侧坐席支撑服务实现：SIP 登录/注销、BYE 挂断与纯 SIP 外呼。
 *
 * <p>迁移自旧 service.fs.impl.AgentServiceImpl：旧 IService/ServiceImpl 基类改为
 * mapper 直注，旧 RestResult 响应包装统一替换为协议层 RestResultEvent，
 * 旧借用 video 基础包的 InviteErrorCode 改用本模块枚举。仅在
 * freeswitch.enabled=true 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentVoService agentVoService;
    private final SIPCommanderForPlatform sipCommanderForPlatform;
    private final SipServer sipServer;

    /**
     * 根据用户ID获取绑定的最新客服。
     *
     * @param userId 用户主键
     * @return 座席实体，未绑定返回 null
     */
    @Override
    public Agent findUserId(Long userId) {
        return agentMapper.findUserId(userId);
    }

    /**
     * 根据 SIP 号获取 FreeSWITCH XML 鉴权模型。
     *
     * @param sip SIP 号码
     * @return 鉴权模型，未找到返回 null
     */
    @Override
    public UserModel findUserModel(String sip) {
        return agentMapper.findUserModel(sip);
    }

    /**
     * 坐席登录：向 FreeSWITCH 发起 SIP REGISTER。
     *
     * @param agentKey 坐席账户
     * @param event    登录结果回调，可为 null
     */
    @Override
    public void login(String agentKey, ResultEvent event) {
        AgentVoInfo agentVoInfo = agentVoService.getAgentByKey(agentKey);
        if (agentVoInfo == null) {
            if (event != null) {
                event.result(new RestResultEvent(RespCode.CODE_2.getValue(), "未获取客服信息"));
            }
            return;
        }
        SipService.getParentPlatformService().login(agentVoInfo,
                ok -> {
                    if (event != null) {
                        event.result(new RestResultEvent(RespCode.CODE_0.getValue(), "登陆成功"));
                    }
                },
                error -> {
                    if (event != null) {
                        event.result(new RestResultEvent(RespCode.CODE_2.getValue(), error.getMsg()));
                    }
                });
    }

    /**
     * 坐席登出：向 FreeSWITCH 发起 SIP UNREGISTER。
     *
     * @param agentKey 坐席账户
     * @param event    登出结果回调，可为 null
     */
    @Override
    public void logout(String agentKey, ResultEvent event) {
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            if (event != null) {
                event.result(new RestResultEvent(RespCode.CODE_2.getValue(), "未获取客服信息"));
            }
            return;
        }
        SipService.getParentPlatformService().unregister(agentVoInfo,
                ok -> {
                    if (event != null) {
                        event.result(new RestResultEvent(RespCode.CODE_0.getValue(), "退出成功"));
                    }
                },
                error -> {
                    if (event != null) {
                        event.result(new RestResultEvent(RespCode.CODE_2.getValue(), error.getMsg()));
                    }
                });
    }

    /**
     * 挂断通话：通过 callId 查找所有关联 SsrcTransaction，向 FreeSWITCH 发送 SIP BYE。
     *
     * @param callId 通话编号
     * @return 处理结果事件
     */
    @Override
    public RestResultEvent stopStream(String callId) {
        List<SsrcTransaction> paramAll = RedisService.getSsrcTransactionManager()
                .getParamAll(null, callId, null, null);
        if (paramAll == null || paramAll.isEmpty()) {
            return new RestResultEvent(RespCode.CODE_0.getValue(), RespCode.CODE_0.getName());
        }
        for (SsrcTransaction ssrcTransaction : paramAll) {
            AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(ssrcTransaction.getAgentKey());
            if (agentVoInfo != null) {
                try {
                    sipCommanderForPlatform.streamByeCmd(sipServer, agentVoInfo,
                            null, null, callId, null, null, null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[BYE 发送失败] 坐席: {} callId: {} 错误: {}",
                            ssrcTransaction.getAgentKey(), callId, e.getMessage());
                }
            }
        }
        return new RestResultEvent(RespCode.CODE_0.getValue(), RespCode.CODE_0.getName());
    }

    /**
     * 纯 SIP 模式呼叫：直接将 SDP 透传给 FreeSWITCH，由其与客户端直接协商 RTP 媒体。
     *
     * @param typeName    通话类型名称
     * @param sipServer   SIP 服务
     * @param agentVoInfo 坐席信息
     * @param caller      被叫号码
     * @param sdp         SIP SDP 内容
     * @param callBackId  回调 ID
     * @param callback    呼叫结果回调
     */
    @Override
    public void callPhone(String typeName, SipServer sipServer, AgentVoInfo agentVoInfo,
                          String caller, String sdp, String callBackId,
                          InviteErrorCallback<Object> callback) {
        if (agentVoInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(), "未获取坐席信息", null);
            return;
        }
        try {
            SIPRequest request = sipCommanderForPlatform.callPhoneSip(sipServer, agentVoInfo, sdp, caller,
                    ok -> {
                        log.info("[SIP 呼叫 200 OK] 坐席: {} 被叫: {}", agentVoInfo.getAgentKey(), caller);
                        callback.run(InviteErrorCode.SUCCESS.getCode(),
                                InviteErrorCode.SUCCESS.getMsg(), null);
                    },
                    error -> {
                        log.error("[SIP 呼叫失败] 坐席: {} 被叫: {} 错误: {} {}",
                                agentVoInfo.getAgentKey(), caller, error.getStatusCode(), error.getMsg());
                        callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(),
                                String.format("呼叫失败: %s %s", error.getStatusCode(), error.getMsg()), null);
                    });
            if (request != null) {
                RedisService.getAgentInfoManager().putCallPhone(
                        request.getCallId().getCallId(), request);
            }
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[SIP INVITE 发送失败] 坐席: {} 被叫: {} 错误: {}",
                    agentVoInfo.getAgentKey(), caller, e.getMessage());
            callback.run(InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
        }
    }
}
