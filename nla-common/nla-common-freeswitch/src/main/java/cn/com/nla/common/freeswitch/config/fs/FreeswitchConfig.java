package cn.com.nla.common.freeswitch.config.fs;


import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.client.fs.handler.process.*;
import cn.com.nla.common.freeswitch.client.fs.handler.strategy.CallStrategyHandler;
import link.thingscloud.freeswitch.esl.InboundClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Import({DynamicTask.class})
@ComponentScan("cn.com.nla.common.freeswitch")
public class FreeswitchConfig {

    //注册IVR处理类
    @Resource
    private InboundClient inboundClient;
    @Resource
    private DynamicTask dynamicTask;
    //流程注册
    @Bean
    public ProcessNextHandler processNextHandler(){
        return new ProcessNextHandler(new CallStrategyHandler(inboundClient));
    }

}
