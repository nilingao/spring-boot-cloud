package cn.com.nla.gen.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.gen.domain.GenTableColumn;

/**
 * 业务字段 数据层
 *
 * @author TZY
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface GenTableColumnMapper extends BaseMapperPlus<GenTableColumn, GenTableColumn> {

}
