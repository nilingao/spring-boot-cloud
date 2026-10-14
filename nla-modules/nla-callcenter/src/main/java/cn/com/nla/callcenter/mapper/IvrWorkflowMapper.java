package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.IvrWorkflow;
import cn.com.nla.callcenter.domain.vo.IvrWorkflowVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** ivr流程表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface IvrWorkflowMapper extends BaseMapperPlus<IvrWorkflow, IvrWorkflowVo> {
}
