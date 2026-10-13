package cn.com.nla.callcenter.spi;

import cn.com.nla.common.freeswitch.model.fs.GroupMemoryInfo;
import cn.com.nla.common.freeswitch.service.freeswitch.GroupMemoryInfoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 坐席客户记忆查询协议侧实现。
 *
 * <p>旧实现即为空存根（坐席-客户记忆域未建），照搬保留排队引擎调用契约；
 * 记忆查询待后续记忆域接入时补齐。仅在 freeswitch.enabled=true 时注册，
 * 避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Component
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class GroupMemoryInfoServiceImpl extends GroupMemoryInfoService {

    /**
     * 查询分组中坐席与客户记忆。
     *
     * @param groupId 技能组编号
     * @param phone   客户号码
     * @return 记忆信息，未接入时返回 null
     */
    @Override
    public GroupMemoryInfo find(String groupId, String phone) {
        return null;
    }
}
