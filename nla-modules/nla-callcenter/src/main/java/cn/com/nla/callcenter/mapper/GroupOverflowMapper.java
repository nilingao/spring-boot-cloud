package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.GroupOverflow;
import cn.com.nla.callcenter.domain.vo.GroupOverflowVo;
import cn.com.nla.callcenter.domain.vo.OverflowConfigVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 技能组排队策略表 Mapper。
 * @author TZY
 */
public interface GroupOverflowMapper extends BaseMapperPlus<GroupOverflow, GroupOverflowVo> {
    /** 指定企业和技能组的有效溢出配置，按关联优先级/ID排序，返回配置主键。 */
    List<OverflowConfigVo> selectConfigsByGroup(@Param("companyId") Long companyId, @Param("groupId") Long groupId);
}
