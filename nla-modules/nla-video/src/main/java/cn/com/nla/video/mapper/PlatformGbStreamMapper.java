package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.PlatformGbStream;
import cn.com.nla.video.domain.vo.PlatformGbStreamVo;
import cn.com.nla.video.domain.vo.GbStreamVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 国标级联关联直播流 Mapper。
 * @author TZY
 */
public interface PlatformGbStreamMapper extends BaseMapperPlus<PlatformGbStream, PlatformGbStreamVo> {
    /** 指定平台国标编号及目录的有效关联；null/空目录集合返回空结果，返回值不携带凭据。 */
    List<GbStreamVo> selectSharedStreams(@Param("platformId") String platformId,
        @Param("catalogIds") List<String> catalogIds);
}
