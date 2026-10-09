package cn.com.nla.common.freeswitch.service;

import cn.com.nla.common.freeswitch.service.freeswitch.AgentVoService;
import cn.com.nla.common.freeswitch.service.freeswitch.CallCdrService;
import cn.com.nla.common.freeswitch.service.freeswitch.GroupMemoryInfoService;
import cn.com.nla.common.freeswitch.service.freeswitch.SendAgentMessage;
import cn.hutool.extra.spring.SpringUtil;

public class FsService {
    public static AgentVoService getAgentService(){return SpringUtil.getBean(AgentVoService.class);}
    public static CallCdrService getCallCdrService(){return SpringUtil.getBean(CallCdrService.class);}
    public static GroupMemoryInfoService getGroupMemoryInfoService(){return SpringUtil.getBean(GroupMemoryInfoService.class);}
    public static SendAgentMessage getSendAgentMessage(){return SpringUtil.getBean(SendAgentMessage.class);}
}
