package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.CompanyConference;
import cn.com.nla.callcenter.domain.vo.CompanyConferenceVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 企业会议室表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface CompanyConferenceMapper extends BaseMapperPlus<CompanyConference, CompanyConferenceVo> {
}
