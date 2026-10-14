package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.StreamProxy;
import cn.com.nla.video.domain.vo.StreamProxyVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 拉流代理的信息 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface StreamProxyMapper extends BaseMapperPlus<StreamProxy, StreamProxyVo> {
}
