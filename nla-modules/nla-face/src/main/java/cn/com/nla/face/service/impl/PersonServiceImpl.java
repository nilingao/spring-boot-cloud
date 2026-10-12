package cn.com.nla.face.service.impl;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.mybatis.core.query.QueryBuilder;
import cn.com.nla.face.core.FaceFeatureCache;
import cn.com.nla.face.core.FaceImageDownloader;
import cn.com.nla.face.core.FacePipeline;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.domain.bo.PersonBo;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.mapper.PersonMapper;
import cn.com.nla.face.service.IPersonService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 人员信息 Service 实现：写入时经识别流水线自动生成人脸特征，并同步维护本地特征缓存。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PersonServiceImpl implements IPersonService {

    private final PersonMapper personMapper;
    private final FacePipeline pipeline;
    private final FaceImageDownloader downloader;
    private final FaceFeatureCache featureCache;

    /**
     * 查询人员详情。
     *
     * @param id 主键
     * @return 人员视图对象
     */
    @Override
    public PersonVo queryById(Long id) {
        return personMapper.selectVoById(id);
    }

    /**
     * 分页查询人员列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 人员分页列表
     */
    @Override
    public PageResult<PersonVo> queryPageList(PersonBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Person> lqw = buildQueryWrapper(bo);
        Page<PersonVo> result = personMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询符合条件的人员列表。
     *
     * @param bo 查询条件
     * @return 人员列表
     */
    @Override
    public List<PersonVo> queryList(PersonBo bo) {
        return personMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构造人员查询条件。
     *
     * @param bo 筛选条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<Person> buildQueryWrapper(PersonBo bo) {
        return QueryBuilder.lambda(Person.class)
            .eqIfText(Person::getImgId, bo.getImgId())
            .likeIfText(Person::getPersonName, bo.getPersonName())
            .eqIfPresent(Person::getGender, bo.getGender())
            .orderByDesc(Person::getId)
            .build();
    }

    /**
     * 新增人员：图片编号查重，下载图片提取特征后落库并写入缓存。
     *
     * @param bo 人员业务对象
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(PersonBo bo) {
        Person add = MapstructUtils.convert(bo, Person.class);
        validEntityBeforeSave(add, null);
        float[] feature = extractFeature(add.getImgUrl());
        add.setExtract(JsonUtils.toJsonString(feature));
        boolean flag = personMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            featureCache.put(add.getImgId(), feature);
        }
        return flag;
    }

    /**
     * 修改人员：图片地址未变化时保持原特征列不重写；变化时重新提取。
     * 缓存按图片编号键同步迁移。
     *
     * @param bo 人员业务对象
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(PersonBo bo) {
        Person update = MapstructUtils.convert(bo, Person.class);
        Person old = personMapper.lambda()
            .select(Person::getImgId, Person::getImgUrl, Person::getExtract)
            .eq(Person::getId, update.getId())
            .one();
        if (old == null) {
            throw new ServiceException("人员信息不存在");
        }
        validEntityBeforeSave(update, update.getId());
        float[] feature = null;
        if (!StringUtils.equals(old.getImgUrl(), update.getImgUrl())) {
            feature = extractFeature(update.getImgUrl());
            update.setExtract(JsonUtils.toJsonString(feature));
        }
        boolean flag = personMapper.updateById(update) > 0;
        if (flag) {
            boolean imgIdChanged = !StringUtils.equals(old.getImgId(), update.getImgId());
            if (imgIdChanged) {
                featureCache.remove(old.getImgId());
            }
            if (feature != null) {
                featureCache.put(update.getImgId(), feature);
            } else if (imgIdChanged) {
                // 图片未换但编号改了：特征原样迁移到新键；历史脏数据无法解析时重新提取
                try {
                    featureCache.put(update.getImgId(), FaceFeatureCache.parseFeatures(old.getExtract()));
                } catch (RuntimeException e) {
                    log.warn("原特征解析失败，按图片地址重新提取: imgId={}", update.getImgId());
                    featureCache.put(update.getImgId(), extractFeature(update.getImgUrl()));
                }
            }
        }
        return flag;
    }

    /**
     * 保存前校验：图片编号在有效人员内不允许重复（编辑排除自身）。
     *
     * @param entity    待保存实体
     * @param excludeId 编辑时排除的主键，新增传 null
     */
    private void validEntityBeforeSave(Person entity, Long excludeId) {
        var checker = personMapper.lambda().eq(Person::getImgId, entity.getImgId());
        if (excludeId != null) {
            checker.ne(Person::getId, excludeId);
        }
        if (checker.exists()) {
            throw new ServiceException("图片编号已存在，请勿重复录入");
        }
    }

    /**
     * 下载图片并提取人脸特征。
     *
     * @param imgUrl 图片地址
     * @return 人脸特征数组
     */
    private float[] extractFeature(String imgUrl) {
        byte[] bytes = downloader.download(imgUrl);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return pipeline.extract(in);
        } catch (IOException e) {
            throw new ServiceException("图片读取失败");
        }
    }

    /**
     * 批量删除人员并在删除成功后移除对应特征缓存。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验（当前无外部引用，保留扩展点）
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<Person> rows = personMapper.lambda()
            .select(Person::getImgId)
            .in(Person::getId, ids)
            .list();
        boolean flag = personMapper.deleteByIds(ids) > 0;
        if (flag) {
            rows.forEach(row -> featureCache.remove(row.getImgId()));
        }
        return flag;
    }

    /**
     * 按图片编号批量查询有效人员，不加载特征。
     *
     * @param imgIds 图片编号集合
     * @return 人员视图列表
     */
    @Override
    public List<PersonVo> selectByImgIds(Collection<String> imgIds) {
        if (imgIds == null || imgIds.isEmpty()) {
            return List.of();
        }
        List<Person> rows = personMapper.selectImgIdList(new ArrayList<>(imgIds));
        return rows.stream().map(row -> MapstructUtils.convert(row, PersonVo.class)).toList();
    }
}
