package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.GroupMemory;
import cn.com.nla.callcenter.domain.vo.GroupMemoryVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 坐席与客户记忆表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface GroupMemoryMapper extends BaseMapperPlus<GroupMemory, GroupMemoryVo> {
}
