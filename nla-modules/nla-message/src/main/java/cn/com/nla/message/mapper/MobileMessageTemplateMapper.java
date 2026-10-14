package cn.com.nla.message.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.vo.MobileMessageTemplateVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/**
 * 短信模板Mapper接口
 *
 * @author TZY
 */
@DS("nla_message")
public interface MobileMessageTemplateMapper extends BaseMapperPlus<MobileMessageTemplate, MobileMessageTemplateVo> {

}
