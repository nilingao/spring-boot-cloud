package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Playback;
import cn.com.nla.callcenter.domain.vo.PlaybackVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 语音文件表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface PlaybackMapper extends BaseMapperPlus<Playback, PlaybackVo> {
}
