package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.VdnDtmf;
import cn.com.nla.callcenter.domain.vo.VdnDtmfVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 路由按键导航表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface VdnDtmfMapper extends BaseMapperPlus<VdnDtmf, VdnDtmfVo> {
}
