package cn.com.nla.system.listener;

import cn.hutool.core.lang.tree.Tree;
import lombok.RequiredArgsConstructor;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.core.utils.TreeBuildUtils;
import cn.com.nla.common.excel.core.ExcelOptionsProvider;
import cn.com.nla.system.domain.bo.SysDeptBo;
import cn.com.nla.system.service.ISysDeptService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Excel 部门下拉选项数据源
 *
 * @author TZY
 */
@Component
@RequiredArgsConstructor
public class DeptExcelOptions implements ExcelOptionsProvider {

    /**
     * 获取下拉选项数据
     *
     * @return 下拉选项列表
     */
    @Override
    public Set<String> getOptions() {
        ISysDeptService deptService = SpringUtils.getBean(ISysDeptService.class);
        List<Tree<Long>> trees = deptService.selectDeptTreeList(new SysDeptBo());
        return TreeBuildUtils.buildTreeNodeMap(trees, "/", Tree::getName).keySet();
    }

}
