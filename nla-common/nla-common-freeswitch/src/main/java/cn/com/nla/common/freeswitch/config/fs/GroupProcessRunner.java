package cn.com.nla.common.freeswitch.config.fs;

import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.service.FsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 进入到队列的电话，需要定时找空闲坐席
 */
@Slf4j
@Component
public class GroupProcessRunner implements CommandLineRunner {

    @Resource
    private DynamicTask dynamicTask;
    @Override
    public void run(String... args) throws Exception {
        //进入到队列的电话，需要定时找空闲坐席
        this.dynamicTask.startCron("GROUP_HANDLER_TASK",5,2, ()->{
            FsService.getGroupMemoryInfoService().execute();
        });
    }
}
