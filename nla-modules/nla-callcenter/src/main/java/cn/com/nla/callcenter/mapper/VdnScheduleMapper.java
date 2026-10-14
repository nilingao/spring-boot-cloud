package cn.com.nla.callcenter.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.callcenter.domain.VdnSchedule;
import cn.com.nla.callcenter.domain.vo.VdnScheduleVo;
import com.baomidou.dynamic.datasource.annotation.DS;

/** 日程表 Mapper。
 * @author TZY
 */
@DS("nla_callcenter")
public interface VdnScheduleMapper extends BaseMapperPlus<VdnSchedule, VdnScheduleVo> {
}
