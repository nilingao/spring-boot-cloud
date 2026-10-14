package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.CompanyPhoneGroup;
import cn.com.nla.callcenter.domain.vo.CompanyPhoneGroupVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 企业号码与号码池中间表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CompanyPhoneGroupMapper extends BaseMapperPlus<CompanyPhoneGroup, CompanyPhoneGroupVo> {
}
