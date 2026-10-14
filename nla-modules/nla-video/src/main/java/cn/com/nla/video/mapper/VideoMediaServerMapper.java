package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.VideoMediaServer;
import cn.com.nla.video.domain.vo.VideoMediaServerVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 流媒体服务信息 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface VideoMediaServerMapper extends BaseMapperPlus<VideoMediaServer, VideoMediaServerVo> {
}
