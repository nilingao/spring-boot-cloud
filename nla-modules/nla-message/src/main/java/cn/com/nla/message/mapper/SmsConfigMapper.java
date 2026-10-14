package cn.com.nla.message.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.domain.vo.SmsConfigVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/**
 * 短信渠道配置Mapper接口
 *
 * @author TZY
 */
@DS("nla_message")
public interface SmsConfigMapper extends BaseMapperPlus<SmsConfig, SmsConfigVo> {

}
