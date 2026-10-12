package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.GbStream;
import cn.com.nla.video.domain.vo.GbStreamVo;
import org.apache.ibatis.annotations.Param;

/** 直播流关联国标上级平台 Mapper。
 * @author TZY
 */
public interface GbStreamMapper extends BaseMapperPlus<GbStream, GbStreamVo> {

    /** 查询已共享给指定上级平台的国标流。 */
    GbStream selectStreamByPlatformGbId(@Param("platformId") String platformId, @Param("gbId") String gbId);
}
