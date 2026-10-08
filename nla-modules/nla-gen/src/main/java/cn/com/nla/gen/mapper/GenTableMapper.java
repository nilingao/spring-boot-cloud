package cn.com.nla.gen.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.gen.domain.GenTable;

import java.util.List;

/**
 * 业务 数据层
 *
 * @author TZY
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface GenTableMapper extends BaseMapperPlus<GenTable, GenTable> {

    /**
     * 查询指定数据源下的所有表名列表
     *
     * @param dataName 数据源名称，用于选择不同的数据源
     * @return 当前数据库中的表名列表
     * @DS("") 使用默认数据源执行查询操作
     */
    @DS("")
    List<String> selectTableNameList(String dataName);
}
