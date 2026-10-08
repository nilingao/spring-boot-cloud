package cn.com.nla.demo.mapper;

import cn.com.nla.common.mybatis.annotation.DataColumn;
import cn.com.nla.common.mybatis.annotation.DataPermission;
import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.demo.domain.TestTree;
import cn.com.nla.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author TZY
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
