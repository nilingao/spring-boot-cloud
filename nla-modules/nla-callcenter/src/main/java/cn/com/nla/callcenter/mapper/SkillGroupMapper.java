package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.SkillGroup;
import cn.com.nla.callcenter.domain.vo.SkillGroupVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 技能组技能表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface SkillGroupMapper extends BaseMapperPlus<SkillGroup, SkillGroupVo> {
}
