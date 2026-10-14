package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.PlatformCatalog;
import cn.com.nla.video.domain.vo.PlatformCatalogVo;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 国标级联-目录 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface PlatformCatalogMapper extends BaseMapperPlus<PlatformCatalog, PlatformCatalogVo> {

    /** 查询目录详情，附带子目录数量。 */
    cn.com.nla.common.video.basic.vo.video.PlatformCatalogVo selectCatalogDetail(@Param("id") String id);

    /** 查询平台下的目录并伪装为通道节点，供上级平台目录树推送。 */
    List<cn.com.nla.common.video.basic.vo.video.DeviceChannelVo> selectCatalogChannelListInPlatform(
        @Param("serverGbId") String serverGbId);
}
