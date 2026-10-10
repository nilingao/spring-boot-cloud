package cn.com.nla.system.service;

import cn.com.nla.system.domain.bo.SysSocialBo;
import cn.com.nla.system.domain.vo.SysSocialVo;

import java.util.List;

/**
 * 社会化关系Service接口
 *
 * @author TZY
 */
public interface ISysSocialService {


    /**
     * 查询社会化关系
     */
    SysSocialVo queryById(String id);

    /**
     * 查询社会化关系列表
     */
    List<SysSocialVo> queryList(SysSocialBo bo);

    /**
     * 查询社会化关系列表
     */
    List<SysSocialVo> queryListByUserId(Long userId);

    /**
     * 新增授权关系
     */
    Boolean insertByBo(SysSocialBo bo);

    /**
     * 更新社会化关系
     */
    Boolean updateByBo(SysSocialBo bo);

    /**
     * 删除社会化关系信息
     */
    Boolean deleteWithValidById(Long id);

    /**
     * 仅删除指定用户拥有的社会化关系，以同一条 SQL 检查归属并删除。
     *
     * @param id 绑定主键
     * @param userId 所属用户主键
     * @return 删除成功返回 {@code true}，缺失或不属于该用户返回 {@code false}
     */
    Boolean deleteByIdAndUserId(Long id, Long userId);


    /**
     * 根据 authId 查询
     */
    List<SysSocialVo> selectByAuthId(String authId);


}
