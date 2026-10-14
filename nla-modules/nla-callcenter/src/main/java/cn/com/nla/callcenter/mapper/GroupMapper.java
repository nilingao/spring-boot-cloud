package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Group;
import cn.com.nla.callcenter.domain.vo.GroupVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 技能组表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface GroupMapper extends BaseMapperPlus<Group, GroupVo> {
}
