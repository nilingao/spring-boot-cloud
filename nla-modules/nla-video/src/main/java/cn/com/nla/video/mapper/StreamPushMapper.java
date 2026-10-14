package cn.com.nla.video.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.video.domain.StreamPush;
import cn.com.nla.video.domain.vo.StreamPushVo;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 推流信息 Mapper。
 * @author TZY
 */
@DS("nla_video")
public interface StreamPushMapper extends BaseMapperPlus<StreamPush, StreamPushVo> {

    /** 查询挂在指定流媒体下且未登记为国标流的推流记录。 */
    List<StreamPush> selectNotSharedByMediaServer(@Param("mediaServerId") String mediaServerId);
}
