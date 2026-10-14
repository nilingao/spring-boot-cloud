package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.CompanyDisplay;
import cn.com.nla.callcenter.domain.vo.CompanyDisplayVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 号码池表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CompanyDisplayMapper extends BaseMapperPlus<CompanyDisplay, CompanyDisplayVo> {
}
