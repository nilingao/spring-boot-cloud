package cn.com.nla.callcenter.config.fs.runtime;

/**
 * 企业呼叫中心运行时配置服务。
 *
 * @author TZY
 */
public interface FsRuntimeConfigService {

    /**
     * 启动时全量初始化，清空所有 Manager 后重建所有企业缓存。
     */
    void refreshAll();

    /**
     * 定向刷新单个企业的运行时配置；企业被禁用或不存在时清除缓存。
     *
     * @param companyId 企业编号
     */
    void refreshCompany(Long companyId);
}
