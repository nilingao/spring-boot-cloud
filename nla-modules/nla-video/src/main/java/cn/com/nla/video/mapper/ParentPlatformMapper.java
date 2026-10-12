package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.common.video.basic.vo.sip.ChannelSourceInfo;
import cn.com.nla.video.domain.ParentPlatform;
import cn.com.nla.video.domain.vo.ParentPlatformVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 平台信息 Mapper。
 * @author TZY
 */
public interface ParentPlatformMapper extends BaseMapperPlus<ParentPlatform, ParentPlatformVo> {

    /** 统计某个国标编号在指定平台下命中的通道来源与国标流来源。 */
    List<ChannelSourceInfo> selectChannelSource(@Param("serverGbId") String serverGbId,
        @Param("channelId") String channelId);

    /** 查询共享了指定通道且处于在线状态的上级平台。 */
    List<ParentPlatform> selectPlatformListForChannel(@Param("channelId") String channelId,
        @Param("allPlatformId") List<String> allPlatformId);

    /** 查询共享了指定国标流的上级平台。 */
    List<ParentPlatform> selectPlatformListForStream(@Param("app") String app, @Param("stream") String stream,
        @Param("allPlatformId") List<String> allPlatformId);
}
