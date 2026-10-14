package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.VdnPhone;
import cn.com.nla.callcenter.domain.vo.VdnPhoneVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 路由号码表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface VdnPhoneMapper extends BaseMapperPlus<VdnPhone, VdnPhoneVo> {
}
