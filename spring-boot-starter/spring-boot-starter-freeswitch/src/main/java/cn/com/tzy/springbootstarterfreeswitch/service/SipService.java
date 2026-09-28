package cn.com.tzy.springbootstarterfreeswitch.service;

import cn.com.tzy.springbootstarterfreeswitch.service.sip.ParentPlatformService;
import cn.hutool.extra.spring.SpringUtil;

public class SipService {
    public static ParentPlatformService getParentPlatformService() {
        return SpringUtil.getBean(ParentPlatformService.class);
    }
}
