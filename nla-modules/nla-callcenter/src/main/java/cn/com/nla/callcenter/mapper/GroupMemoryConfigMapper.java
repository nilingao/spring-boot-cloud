package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.GroupMemoryConfig;
import cn.com.nla.callcenter.domain.vo.GroupMemoryConfigVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 技能组坐席记忆配置表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface GroupMemoryConfigMapper extends BaseMapperPlus<GroupMemoryConfig, GroupMemoryConfigVo> {
}
