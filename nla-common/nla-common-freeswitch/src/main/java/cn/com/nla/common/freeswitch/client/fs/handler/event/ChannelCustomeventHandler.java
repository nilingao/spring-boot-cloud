package cn.com.nla.common.freeswitch.client.fs.handler.event;

import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.model.fs.CompanyInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteGateWayInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteGroupInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.service.FsService;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.time.LocalDateTime;

/**
 *  用户注册注销事件
 */
@Slf4j
@Component
@Order(30)
@EslEventName(EventNames.CUSTOM)
public class ChannelCustomeventHandler implements EslEventHandler {
    //开始缓存用户信息
    @Override
    public void handle(String addr, EslEvent event) {
        String userName = event.getEventHeaders().get("user_name");//主叫号码
        String eventSubclass = event.getEventHeaders().get("Event-Subclass");//相关事件

        if ("sofia::register".equals(eventSubclass)) {
            log.info("进入事件 [ 用户注册 ] CUSTOM");
            String networkIp = event.getEventHeaders().get("network-ip");
            String networkPort = event.getEventHeaders().get("network-port");
            //注册
            AgentVoInfo agentVoInfo = FsService.getAgentService().getAgentBySip(userName);
            if (agentVoInfo != null) {
                AgentVoInfo redisAgentVoInfo = RedisService.getAgentInfoManager().get(agentVoInfo.getAgentKey());
                if(redisAgentVoInfo != null){
                    agentVoInfo = redisAgentVoInfo;
                }
                String[] split = addr.split(":");
                agentVoInfo.setRenewTime(LocalDateTime.now());
                agentVoInfo.setRegisterTime(LocalDateTime.now());
                agentVoInfo.setFsHost(split[0]);
                agentVoInfo.setFsPost(split[1]);
                agentVoInfo.setAgentState(AgentStateEnum.READY);
                agentVoInfo.setSipPhone(userName);
                FsService.getAgentService().online(agentVoInfo, null);
                CompanyInfo companyInfo = RedisService.getCompanyInfoManager().get(agentVoInfo.getCompanyId());
                if (companyInfo != null) {
                    RouteGroupInfo routeGroupInfo = companyInfo.getRouteGroupMap().computeIfAbsent(agentVoInfo.getAgentId(), k -> new RouteGroupInfo());
                    routeGroupInfo.setRouteGateWayInfoList(Collections.singletonList(RouteGateWayInfo.builder()
                            .name(String.format("坐席AgentId：%s", agentVoInfo.getAgentId()))
                            .mediaHost(networkIp)
                            .profile("internal")
                            .mediaPort(Integer.valueOf(networkPort))
                            .build()));
                    RedisService.getCompanyInfoManager().put(companyInfo);
                }

            }
        }else if ("sofia::unregister".equals(eventSubclass)) {
            log.info("进入事件 [ 用户注销 ] CUSTOM");
            //注销
            AgentVoInfo agentBySip = FsService.getAgentService().getAgentBySip(userName);
            if (agentBySip != null) {
                CompanyInfo companyInfo = RedisService.getCompanyInfoManager().get(agentBySip.getCompanyId());
                if (companyInfo != null) {
                    companyInfo.getRouteGroupMap().remove(agentBySip.getAgentId());
                    RedisService.getCompanyInfoManager().put(companyInfo);
                }
                FsService.getAgentService().offline(agentBySip.getAgentKey());
            }
        }
    }
}
