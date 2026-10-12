package cn.com.nla.face.service;

import cn.com.nla.face.domain.vo.FaceDetectVo;
import cn.com.nla.face.domain.vo.PersonVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 人脸识别 Service 接口，能力对齐旧 FaceDetectorService。
 *
 * @author TZY
 */
public interface IFaceDetectorService {

    /**
     * 全属性检测：活体、性别、年龄、双眼状态与口罩状态。
     *
     * @param file 人脸图片
     * @return 检测信息
     */
    FaceDetectVo findFaceInfo(MultipartFile file);

    /**
     * 两张图片的 1:1 人脸相似度比对。
     *
     * @param file1 图片一
     * @param file2 图片二
     * @return 相似度 0~1
     */
    float comparison(MultipartFile file1, MultipartFile file2);

    /**
     * 基于本地特征缓存的 1:N 人脸检索。
     *
     * @param file      待检索图片
     * @param size      返回条数上限，null 或非正数按 1
     * @param calculate 相似度百分比阈值，null 或非正数按 70
     * @return 命中的人员列表；无命中返回空列表
     */
    List<PersonVo> search(MultipartFile file, Integer size, Integer calculate);
}
