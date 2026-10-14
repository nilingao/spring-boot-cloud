package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.FsMediaServer;
import cn.com.nla.callcenter.domain.vo.FsMediaServerVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 流媒体服务信息 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface FsMediaServerMapper extends BaseMapperPlus<FsMediaServer, FsMediaServerVo> {
}
