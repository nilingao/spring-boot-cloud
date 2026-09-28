package cn.com.tzy.springbootfs.service.fs.impl;

import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootentity.dome.fs.Agent;
import cn.com.tzy.springbootfs.mapper.fs.AgentMapper;
import cn.com.tzy.springbootfs.service.fs.AgentService;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.SipServer;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.callback.InviteErrorCallback;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.tzy.springbootstarterfreeswitch.common.interfaces.ResultEvent;
import cn.com.tzy.springbootstarterfreeswitch.enums.sip.VideoStreamType;
import cn.com.tzy.springbootstarterfreeswitch.model.bean.UserModel;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstarterfreeswitch.redis.impl.sip.SsrcTransactionManager;
import cn.com.tzy.springbootstarterfreeswitch.service.SipService;
import cn.com.tzy.springbootstarterfreeswitch.service.freeswitch.AgentVoService;
import cn.com.tzy.springbootstarterfreeswitch.vo.sip.SsrcTransaction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import cn.com.tzy.springbootstartervideobasic.enums.InviteErrorCode;

import javax.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

@Log4j2
@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    @Resource
    private AgentVoService agentVoService;
    @Resource
    private SIPCommanderForPlatform sipCommanderForPlatform;
    @Resource
    protected SipServer sipServer;

    @Override
    public Agent findUserId(Long userId) {
        return baseMapper.findUserId(userId);
    }

    @Override
    public UserModel findUserModel(String sip) {
        return baseMapper.findUserModel(sip);
    }

    @Override
    public void login(String agentKey, ResultEvent event) {
        AgentVoInfo agentVoInfo = agentVoService.getAgentByKey(agentKey);
        if (agentVoInfo == null) {
            if (event != null) {
                event.result(RestResult.result(RespCode.CODE_2.getValue(), "未获取客服信息"));
            }
            return;
        }
        SipService.getParentPlatformService().login(agentVoInfo,
                ok -> {
                    if (event != null) {
                        event.result(RestResult.result(RespCode.CODE_0.getValue(), "登陆成功"));
                    }
                },
                error -> {
                    if (event != null) {
                        event.result(RestResult.result(RespCode.CODE_2.getValue(), error.getMsg()));
                    }
                });
    }

    @Override
    public void logout(String agentKey, ResultEvent event) {
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            if (event != null) {
                event.result(RestResult.result(RespCode.CODE_2.getValue(), "未获取客服信息"));
            }
            return;
        }
        SipService.getParentPlatformService().unregister(agentVoInfo,
                ok -> {
                    if (event != null) {
                        event.result(RestResult.result(RespCode.CODE_0.getValue(), "退出成功"));
                    }
                },
                error -> {
                    if (event != null) {
                        event.result(RestResult.result(RespCode.CODE_2.getValue(), error.getMsg()));
                    }
                });
    }

    /**
     * 挂断通话：通过 callId 查找所有关联 SsrcTransaction，向 FreeSWITCH 发送 SIP BYE。
     */
    @Override
    public RestResult<?> stopStream(String callId) {
        List<SsrcTransaction> paramAll = RedisService.getSsrcTransactionManager()
                .getParamAll(null, callId, null, null);
        if (paramAll == null || paramAll.isEmpty()) {
            return RestResult.result(RespCode.CODE_0);
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
        return RestResult.result(RespCode.CODE_0);
    }

    /**
     * 纯 SIP 模式呼叫。
     * 直接将 SDP 透传给 FreeSWITCH，不经过 ZLM，由 FreeSWITCH 与客户端直接协商 RTP 媒体。
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
