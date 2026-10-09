package cn.com.nla.system.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.system.domain.SysArea;
import cn.com.nla.system.domain.vo.SysAreaVo;

/**
 * 行政区划 数据层
 * <p>
 * 行政区划为全局只读参考数据，不涉及数据权限，故不标注 {@code @DataPermission}。
 *
 * @author TZY
 */
public interface SysAreaMapper extends BaseMapperPlus<SysArea, SysAreaVo> {

}
