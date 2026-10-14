package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.VdnCode;
import cn.com.nla.callcenter.domain.vo.VdnCodeVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 呼入路由表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface VdnCodeMapper extends BaseMapperPlus<VdnCode, VdnCodeVo> {
}
