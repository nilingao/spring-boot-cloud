package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.PlatformGbChannel;
import cn.com.nla.video.domain.vo.PlatformGbChannelVo;
import cn.com.nla.video.domain.vo.DeviceChannelVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 国标级联关联通道信息 Mapper。
 * @author TZY
 */
public interface PlatformGbChannelMapper extends BaseMapperPlus<PlatformGbChannel, PlatformGbChannelVo> {
    /** 指定平台国标编号及目录的有效关联；null/空目录集合返回空结果，返回值不携带凭据。 */
    List<DeviceChannelVo> selectSharedChannels(@Param("platformId") String platformId,
        @Param("catalogIds") List<String> catalogIds);
}
