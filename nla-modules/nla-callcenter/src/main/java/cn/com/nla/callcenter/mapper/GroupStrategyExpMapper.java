package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.GroupStrategyExp;
import cn.com.nla.callcenter.domain.vo.GroupStrategyExpVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 技能组坐席自定义策略表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface GroupStrategyExpMapper extends BaseMapperPlus<GroupStrategyExp, GroupStrategyExpVo> {
}
