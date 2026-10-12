package cn.com.nla.face.core;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.face.domain.vo.FaceDetectVo;
import cn.com.nla.common.facesdk.core.proxy.AgePredictorProxy;
import cn.com.nla.common.facesdk.core.proxy.EyeStateDetectorProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceAntiSpoofingProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceDetectorProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceLandmarkerProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceRecognizerProxy;
import cn.com.nla.common.facesdk.core.proxy.GenderPredictorProxy;
import cn.com.nla.common.facesdk.core.proxy.MaskDetectorProxy;
import com.seeta.sdk.EyeStateDetector;
import com.seeta.sdk.GenderPredictor;
import com.seeta.sdk.LandmarkerMask;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;
import com.seeta.sdk.util.SeetafaceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * 人脸检测流水线，移植自旧 FacePool.Builder：检测唯一人脸 → 裁剪 → 特征点 → 各属性评估/特征提取。
 * <p>
 * 算法代理经 {@link ObjectProvider} 延迟获取，{@code face.enabled=false} 时管理 CRUD 仍可启动，
 * 仅识别类调用报业务错误。旧实现右眼状态误用左眼结果的缺陷已修正。
 *
 * @author TZY
 */
@Slf4j
@Component
public class FacePipeline {

    private final ObjectProvider<FaceDetectorProxy> detectorProvider;
    private final ObjectProvider<FaceLandmarkerProxy> landmarkerProvider;
    private final ObjectProvider<FaceAntiSpoofingProxy> antiSpoofingProvider;
    private final ObjectProvider<GenderPredictorProxy> genderProvider;
    private final ObjectProvider<AgePredictorProxy> ageProvider;
    private final ObjectProvider<EyeStateDetectorProxy> eyeStateProvider;
    private final ObjectProvider<MaskDetectorProxy> maskProvider;
    private final ObjectProvider<FaceRecognizerProxy> recognizerProvider;

    public FacePipeline(ObjectProvider<FaceDetectorProxy> detectorProvider,
                        ObjectProvider<FaceLandmarkerProxy> landmarkerProvider,
                        ObjectProvider<FaceAntiSpoofingProxy> antiSpoofingProvider,
                        ObjectProvider<GenderPredictorProxy> genderProvider,
                        ObjectProvider<AgePredictorProxy> ageProvider,
                        ObjectProvider<EyeStateDetectorProxy> eyeStateProvider,
                        ObjectProvider<MaskDetectorProxy> maskProvider,
                        ObjectProvider<FaceRecognizerProxy> recognizerProvider) {
        this.detectorProvider = detectorProvider;
        this.landmarkerProvider = landmarkerProvider;
        this.antiSpoofingProvider = antiSpoofingProvider;
        this.genderProvider = genderProvider;
        this.ageProvider = ageProvider;
        this.eyeStateProvider = eyeStateProvider;
        this.maskProvider = maskProvider;
        this.recognizerProvider = recognizerProvider;
    }

    /**
     * 全属性检测：活体、性别、年龄、双眼状态、口罩状态；不提取识别特征。
     *
     * @param input 图片输入流，读取后由调用方负责关闭
     * @return 检测信息
     */
    public FaceDetectVo analyze(InputStream input) {
        long startTime = System.currentTimeMillis();
        FaceDetectorProxy detector = require(detectorProvider);
        SeetaImageData imageData = readImage(input);
        // 与旧流水线一致：先整图定位并裁剪出人脸，再基于裁剪图做全部属性评估
        SeetaRect rect = requireSingleFace(detector, imageData);
        imageData = cutFace(imageData, rect);
        rect = requireSingleFace(detector, imageData);
        SeetaPointF[] points = landmarks(imageData, rect);

        FaceDetectVo vo = new FaceDetectVo();
        vo.setPredict(require(antiSpoofingProvider).predict(imageData, rect, points).ordinal());
        GenderPredictorProxy.GenderItem gender = require(genderProvider).predictGenderWithCrop(imageData, points);
        vo.setGender(gender.getGender() == GenderPredictor.GENDER.MALE ? 1 : 2);
        vo.setAge(require(ageProvider).predictAgeWithCrop(imageData, points));
        EyeStateDetector.EYE_STATE[] eyes = require(eyeStateProvider).detect(imageData, points);
        vo.setLeftEyeState(toEyeState(eyes[0]));
        vo.setRightEyeState(toEyeState(eyes[1]));
        vo.setMaskStatus(require(maskProvider).detect(imageData, rect).getMask() ? 0 : 1);
        log.info("人脸检测耗时：{}ms", System.currentTimeMillis() - startTime);
        return vo;
    }

    /**
     * 提取人脸 1:N 检索用特征数组（检测唯一人脸 → 裁剪 → 特征点 → extract）。
     *
     * @param input 图片输入流，读取后由调用方负责关闭
     * @return 特征数组
     */
    public float[] extract(InputStream input) {
        FaceDetectorProxy detector = require(detectorProvider);
        SeetaImageData imageData = readImage(input);
        SeetaRect rect = requireSingleFace(detector, imageData);
        imageData = cutFace(imageData, rect);
        rect = requireSingleFace(detector, imageData);
        SeetaPointF[] points = landmarks(imageData, rect);
        return require(recognizerProvider).extract(imageData, points);
    }

    /**
     * 计算两个人脸特征数组的相似度。
     *
     * @param features1 特征数组一
     * @param features2 特征数组二
     * @return 相似度 0~1
     */
    public float compare(float[] features1, float[] features2) {
        return require(recognizerProvider).calculateSimilarity(features1, features2);
    }

    /**
     * 读取图片为 BGR 像素数据；无法解析时拒绝。
     *
     * @param input 图片输入流
     * @return SDK 图片数据
     */
    private SeetaImageData readImage(InputStream input) {
        try {
            return SeetafaceUtil.toSeetaImageData(input);
        } catch (RuntimeException e) {
            throw new ServiceException("无法读取图片，请上传有效的图片文件");
        }
    }

    /**
     * 检测并保证图片中恰好存在一张人脸。
     *
     * @param detector   人脸检测器
     * @param imageData  图片数据
     * @return 唯一人脸位置
     */
    private SeetaRect requireSingleFace(FaceDetectorProxy detector, SeetaImageData imageData) {
        SeetaRect[] data;
        try {
            data = detector.detect(imageData);
        } catch (Exception e) {
            log.error("获取人脸质量信息错误:", e);
            throw new ServiceException("获取人脸质量信息错误");
        }
        if (data == null || data.length == 0) {
            throw new ServiceException("未检测到人脸信息");
        }
        if (data.length > 1) {
            throw new ServiceException("检测到多张人脸，请保证图片中只有一人");
        }
        return data[0];
    }

    /**
     * 按人脸位置裁剪并重新编码为 SDK 图片数据。
     *
     * @param imageData 原图数据
     * @param rect      人脸位置
     * @return 裁剪后的图片数据
     */
    private SeetaImageData cutFace(SeetaImageData imageData, SeetaRect rect) {
        BufferedImage bufferedImage = SeetafaceUtil.toBufferedImage(imageData);
        return SeetafaceUtil.toSeetaImageData(SeetafaceUtil.writeCut(bufferedImage, rect));
    }

    /**
     * 获取人脸特征点，附带口罩标记数组。
     *
     * @param imageData 图片数据
     * @param rect      人脸位置
     * @return 特征点数组
     */
    private SeetaPointF[] landmarks(SeetaImageData imageData, SeetaRect rect) {
        LandmarkerMask mask = require(landmarkerProvider).isMask(imageData, rect);
        if (mask == null || mask.getMasks() == null || mask.getMasks().length == 0) {
            log.error("未获取人脸特质点");
            throw new ServiceException("未获取人脸特质点");
        }
        return mask.getSeetaPointFS();
    }

    /**
     * 眼睛状态枚举映射为旧接口数值：0 闭眼、1 睁眼、2 非眼部区域、3 未知。
     *
     * @param state 眼睛状态枚举
     * @return 状态数值
     */
    private int toEyeState(EyeStateDetector.EYE_STATE state) {
        if (state == EyeStateDetector.EYE_STATE.EYE_CLOSE) {
            return 0;
        }
        if (state == EyeStateDetector.EYE_STATE.EYE_OPEN) {
            return 1;
        }
        return state == EyeStateDetector.EYE_STATE.EYE_RANDOM ? 2 : 3;
    }

    /**
     * 延迟获取算法代理，未启用人脸功能时报业务错误。
     *
     * @param provider 代理提供者
     * @return 代理实例
     */
    private <T> T require(ObjectProvider<T> provider) {
        T proxy = provider.getIfAvailable();
        if (proxy == null) {
            throw new ServiceException("人脸识别功能未启用，请先配置 face.enabled=true");
        }
        return proxy;
    }
}
