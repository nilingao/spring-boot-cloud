package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Company;
import cn.com.nla.callcenter.domain.vo.CompanyVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 企业信息表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CompanyMapper extends BaseMapperPlus<Company, CompanyVo> {
}
