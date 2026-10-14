package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.Skill;
import cn.com.nla.callcenter.domain.vo.SkillVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 技能表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface SkillMapper extends BaseMapperPlus<Skill, SkillVo> {
}
