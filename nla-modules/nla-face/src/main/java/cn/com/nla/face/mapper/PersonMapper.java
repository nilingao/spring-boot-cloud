package cn.com.nla.face.mapper;

import cn.com.nla.common.mybatis.core.mapper.BaseMapperPlus;
import cn.com.nla.face.domain.Person;
import cn.com.nla.face.domain.vo.PersonVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 人员 Mapper；普通查询与内部特征读取分开。
 *
 * @author TZY
 */
public interface PersonMapper extends BaseMapperPlus<Person, PersonVo> {
    /** 按图片编号查询有效人员，不加载特征；null 或空集合返回空结果。 */
    List<Person> selectImgIdList(@Param("imgIdList") List<String> imgIdList);

    /** 内部特征缓存初始化专用，仅加载有效人员的 id、图片编号与非空特征。 */
    List<Person> selectFeatureList();
}
