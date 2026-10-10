package cn.com.nla.common.facesdk.core.config;

import cn.com.nla.common.facesdk.core.properties.FaceProperties;
import com.seeta.sdk.util.LoadNativeCore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Fails startup before JNI invocation when native/model configuration is incomplete. */
public class FaceSdkNativeLoader {
    private static final List<String> MODELS = List.of("face_detector.csta", "face_landmarker_pts5.csta",
        "age_predictor.csta", "eye_state.csta", "fas_first.csta", "face_recognizer.csta",
        "gender_predictor.csta", "mask_detector.csta", "pose_estimation.csta", "quality_lbn.csta");
    private final FaceProperties properties;
    public FaceSdkNativeLoader(FaceProperties properties) { this.properties = properties; }

    public void load() {
        if (properties.getCstaPath() == null || properties.getCstaPath().isBlank())
            throw new IllegalArgumentException("face.csta-path is required");
        if (properties.getMaxTotal() < 1 || properties.getMaxIdle() < 0 ||
            properties.getMinIdle() < 0 || properties.getMinIdle() > properties.getMaxIdle() ||
            properties.getMaxIdle() > properties.getMaxTotal() || properties.getMaxWait() == null ||
            properties.getMaxWait().isNegative() || properties.getMaxWait().isZero())
            throw new IllegalArgumentException("Invalid face pool limits or max-wait");
        Path directory = Path.of(properties.getCstaPath()).toAbsolutePath().normalize();
        for (String name : MODELS) {
            Path model = directory.resolve(name);
            if (!Files.isRegularFile(model) || !Files.isReadable(model))
                throw new IllegalArgumentException("Missing SeetaFace model: " + model);
        }
        LoadNativeCore.LOAD_NATIVE(properties.getDllPath(), properties.getDevice());
    }
}
