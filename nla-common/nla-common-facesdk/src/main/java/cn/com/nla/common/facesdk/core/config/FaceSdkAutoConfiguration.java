package cn.com.nla.common.facesdk.core.config;

import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import cn.com.nla.common.facesdk.core.properties.FaceProperties;
import cn.com.nla.common.facesdk.core.proxy.*;
import com.seeta.sdk.SeetaModelSetting;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Boot 4 configuration; all native loading is opt-in. */
@AutoConfiguration
@ConditionalOnProperty(prefix = "face", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FaceProperties.class)
public class FaceSdkAutoConfiguration {
    @Bean(initMethod = "load")
    @ConditionalOnMissingBean
    public FaceSdkNativeLoader faceSdkNativeLoader(FaceProperties properties) {
        return new FaceSdkNativeLoader(properties);
    }

    private <T> SeetaConfSetting<T> settings(FaceProperties properties, String model) {
        SeetaConfSetting<T> settings = new SeetaConfSetting<>();
        if (model != null) settings.setSeetaModelSetting(new SeetaModelSetting(properties.getDeviceId(),
            new String[]{Path.of(properties.getCstaPath()).toAbsolutePath().normalize().resolve(model).toString()},
            properties.getDevice()));
        settings.setMaxTotal(properties.getMaxTotal());
        settings.setMaxIdle(properties.getMaxIdle());
        settings.setMinIdle(properties.getMinIdle());
        settings.setMaxWait(properties.getMaxWait());
        settings.setTestOnBorrow(true);
        return settings;
    }

    @Bean
    @ConditionalOnMissingBean
    public FaceDetectorProxy faceDetectorProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new FaceDetectorProxy(settings(properties, "face_detector.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public FaceLandmarkerProxy faceLandmarkerProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new FaceLandmarkerProxy(settings(properties, "face_landmarker_pts5.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public AgePredictorProxy agePredictorProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new AgePredictorProxy(settings(properties, "age_predictor.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public EyeStateDetectorProxy eyeStateDetectorProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new EyeStateDetectorProxy(settings(properties, "eye_state.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public FaceAntiSpoofingProxy faceAntiSpoofingProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new FaceAntiSpoofingProxy(settings(properties, "fas_first.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public FaceRecognizerProxy faceRecognizerProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new FaceRecognizerProxy(settings(properties, "face_recognizer.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public GenderPredictorProxy genderPredictorProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new GenderPredictorProxy(settings(properties, "gender_predictor.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public MaskDetectorProxy maskDetectorProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new MaskDetectorProxy(settings(properties, "mask_detector.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public PoseEstimatorProxy poseEstimatorProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new PoseEstimatorProxy(settings(properties, "pose_estimation.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfLBNProxy qualityOfLBNProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfLBNProxy(settings(properties, "quality_lbn.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfPoseExProxy qualityOfPoseExProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfPoseExProxy(settings(properties, "pose_estimation.csta"));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfBrightnessProxy qualityOfBrightnessProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfBrightnessProxy(settings(properties, null));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfClarityProxy qualityOfClarityProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfClarityProxy(settings(properties, null));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfIntegrityProxy qualityOfIntegrityProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfIntegrityProxy(settings(properties, null));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfPoseProxy qualityOfPoseProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfPoseProxy(settings(properties, null));
    }

    @Bean
    @ConditionalOnMissingBean
    public QualityOfResolutionProxy qualityOfResolutionProxy(FaceSdkNativeLoader loader, FaceProperties properties) {
        return new QualityOfResolutionProxy(settings(properties, null));
    }

}
