package cn.com.nla.face.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.face.core.FaceFeatureCache;
import cn.com.nla.face.core.FacePipeline;
import cn.com.nla.face.domain.vo.FaceDetectVo;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.service.IFaceDetectorService;
import cn.com.nla.face.service.IPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人脸识别 Service 实现：检测/比对/检索三能力移植自旧 FaceDetectorServiceImpl，
 * 检索沿用"全量内存遍历 + 相似度阈值截断"的旧算法与默认参数。
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FaceDetectorServiceImpl implements IFaceDetectorService {

    /** 检索默认返回条数，与旧实现一致。 */
    private static final int DEFAULT_SIZE = 1;
    /** 检索默认相似度百分比阈值，与旧实现一致。 */
    private static final int DEFAULT_CALCULATE = 70;

    private final FacePipeline pipeline;
    private final FaceFeatureCache featureCache;
    private final IPersonService personService;

    /**
     * 全属性检测上传人脸图片。
     *
     * @param file 人脸图片
     * @return 检测信息
     */
    @Override
    public FaceDetectVo findFaceInfo(MultipartFile file) {
        try (InputStream in = open(file)) {
            return pipeline.analyze(in);
        } catch (IOException e) {
            throw new ServiceException("无法读取上传的图片");
        }
    }

    /**
     * 两张图片分别提取特征后计算相似度。
     *
     * @param file1 图片一
     * @param file2 图片二
     * @return 相似度 0~1
     */
    @Override
    public float comparison(MultipartFile file1, MultipartFile file2) {
        float[] features1 = extractOf(file1);
        float[] features2 = extractOf(file2);
        return pipeline.compare(features1, features2);
    }

    /**
     * 提取图片特征并遍历本地特征缓存检索相似人脸，命中图片编号后回填人员信息。
     *
     * @param file      待检索图片
     * @param size      返回条数上限，null 或非正数按 1
     * @param calculate 相似度百分比阈值，null 或非正数按 70
     * @return 命中的人员列表
     */
    @Override
    public List<PersonVo> search(MultipartFile file, Integer size, Integer calculate) {
        float[] features = extractOf(file);
        int limit = (size == null || size <= 0) ? DEFAULT_SIZE : size;
        int threshold = (calculate == null || calculate <= 0) ? DEFAULT_CALCULATE : calculate;
        List<String> imgIdList = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : featureCache.snapshot().entrySet()) {
            float score = pipeline.compare(features, entry.getValue());
            if (threshold <= score * 100) {
                imgIdList.add(entry.getKey());
            }
            if (imgIdList.size() >= limit) {
                break;
            }
        }
        if (imgIdList.isEmpty()) {
            return List.of();
        }
        return personService.selectByImgIds(imgIdList);
    }

    /**
     * 读取上传文件流；文件为空或读取失败时报业务错误。
     *
     * @param file 上传文件
     * @return 文件输入流
     * @throws IOException 读取异常
     */
    private InputStream open(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("人脸图片不能为空");
        }
        return file.getInputStream();
    }

    /**
     * 上传文件提取人脸特征。
     *
     * @param file 上传文件
     * @return 特征数组
     */
    private float[] extractOf(MultipartFile file) {
        try (InputStream in = open(file)) {
            return pipeline.extract(in);
        } catch (IOException e) {
            throw new ServiceException("无法读取上传的图片");
        }
    }
}
