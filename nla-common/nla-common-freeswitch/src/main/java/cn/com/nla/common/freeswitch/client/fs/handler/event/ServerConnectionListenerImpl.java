package cn.com.nla.common.freeswitch.client.fs.handler.event;

import link.thingscloud.freeswitch.esl.ServerConnectionListener;
import link.thingscloud.freeswitch.esl.inbound.option.ServerOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>ServerConnectionListenerImpl class.</p>
 *
 * @author : zhouhailin
 * @version $Id: $Id
 */
@Slf4j
@Component
public class ServerConnectionListenerImpl implements ServerConnectionListener {
    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpened(ServerOption serverOption) {
        log.info("服务开启 onOpened serverOption : {}", serverOption);
    }

    /** {@inheritDoc} */
    @Override
    public void onClosed(ServerOption serverOption) {
        log.info("服务关闭 onClosed serverOption : {}", serverOption);
    }
}