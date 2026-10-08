package cn.com.nla.system.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.system.domain.SysRoleMenu;

import java.util.Collection;

/**
 * 角色与菜单关联表 数据层
 *
 * @author TZY
 */
public interface SysRoleMenuMapper extends BaseMapperPlus<SysRoleMenu, SysRoleMenu> {

    /**
     * 根据菜单ID串删除关联关系
     *
     * @param menuIds 菜单ID串
     * @return 结果
     */
    default int deleteByMenuIds(Collection<Long> menuIds) {
        return this.lambda().in(SysRoleMenu::getMenuId, menuIds).deleteCount();
    }

}
