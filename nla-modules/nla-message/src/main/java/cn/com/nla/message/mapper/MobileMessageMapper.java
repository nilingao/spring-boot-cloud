package cn.com.nla.message.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.message.domain.MobileMessage;
import cn.com.nla.message.domain.vo.MobileMessageVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/**
 * 短信发送记录Mapper接口
 *
 * @author TZY
 */
@DS("nla_message")
public interface MobileMessageMapper extends BaseMapperPlus<MobileMessage, MobileMessageVo> {

}
