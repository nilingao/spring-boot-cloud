package cn.com.nla.message.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.message.domain.MobileMessage;
import cn.com.nla.message.domain.bo.MobileMessageBo;
import cn.com.nla.message.domain.vo.MobileMessageVo;
import cn.com.nla.message.mapper.MobileMessageMapper;
import cn.com.nla.message.service.IMobileMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 短信发送记录Service业务层处理
 * <p>
 * 追加型记录（无逻辑删除，物理删除），仅供查询、导出与清理；记录由发送流程自动落库，不提供新增/修改。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MobileMessageServiceImpl implements IMobileMessageService {

    private final MobileMessageMapper messageMapper;

    /**
     * 分页查询短信发送记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信发送记录分页列表
     */
    @Override
    public PageResult<MobileMessageVo> queryPageList(MobileMessageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MobileMessage> lqw = buildQueryWrapper(bo);
        Page<MobileMessageVo> result = messageMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的短信发送记录列表
     *
     * @param bo 查询条件
     * @return 短信发送记录列表
     */
    @Override
    public List<MobileMessageVo> queryList(MobileMessageBo bo) {
        LambdaQueryWrapper<MobileMessage> lqw = buildQueryWrapper(bo);
        return messageMapper.selectVoList(lqw);
    }

    /**
     * 构造短信发送记录查询条件。
     *
     * @param bo 筛选条件
     * @return 包含手机号、类型、状态、发送人、模板号与创建时间区间的查询包装器
     */
    private LambdaQueryWrapper<MobileMessage> buildQueryWrapper(MobileMessageBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(MobileMessage.class)
            .likeIfText(MobileMessage::getMobile, bo.getMobile())
            .eqIfPresent(MobileMessage::getType, bo.getType())
            .eqIfPresent(MobileMessage::getStatus, bo.getStatus())
            .eqIfPresent(MobileMessage::getSenderId, bo.getSenderId())
            .likeIfText(MobileMessage::getTemplateId, bo.getTemplateId())
            .betweenParams(MobileMessage::getCreateTime, params, "beginTime", "endTime")
            .orderByDesc(MobileMessage::getId)
            .build();
    }

    /**
     * 批量删除短信发送记录
     *
     * @param ids 待删除的主键集合
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return messageMapper.deleteByIds(ids) > 0;
    }

    /**
     * 清空短信发送记录
     */
    @Override
    public void clean() {
        messageMapper.lambda().delete();
    }

}
