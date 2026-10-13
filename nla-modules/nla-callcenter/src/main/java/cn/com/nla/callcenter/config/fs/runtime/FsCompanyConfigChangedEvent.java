package cn.com.nla.callcenter.config.fs.runtime;

import lombok.Getter;

import java.util.Objects;

/**
 * 企业呼叫中心配置变更事件。
 * <p>
 * 由管理域 CRUD 在事务提交后发布, 通知运行时配置链路重建对应企业的 Redis 缓存
 * (企业信息/路由网关/技能组等), 见 {@code FsRuntimeConfigRefreshListener}。
 *
 * @author TZY
 */
@Getter
public class FsCompanyConfigChangedEvent {

    /**
     * 受影响的企业编号。
     */
    private final Long companyId;

    /**
     * 构造企业配置变更事件。
     *
     * @param companyId 受影响的企业编号, 不允许为空
     */
    public FsCompanyConfigChangedEvent(Long companyId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
    }

}
