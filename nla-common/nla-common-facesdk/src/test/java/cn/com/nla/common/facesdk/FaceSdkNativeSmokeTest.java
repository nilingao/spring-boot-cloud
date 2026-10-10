package cn.com.nla.common.facesdk;

import cn.com.nla.common.facesdk.core.config.FaceSdkAutoConfiguration;
import cn.com.nla.common.facesdk.core.proxy.FaceDetectorProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceRecognizerProxy;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.SeetaDevice;
import com.seeta.sdk.SeetaModelSetting;
import com.seeta.sdk.util.LoadNativeCore;
import com.seeta.sdk.util.SeetafaceUtil;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in smoke using externally installed binaries and models. */
@EnabledIfSystemProperty(named = "face.native.path", matches = ".+")
class FaceSdkNativeSmokeTest {
    @Test
    void jdk21LoadsWindowsJniCreatesModelsDetectsAndClosesEveryProxyPool() {
        var handles = new java.util.ArrayList<Object>();
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FaceSdkAutoConfiguration.class))
            .withPropertyValues("face.enabled=true", "face.dll-path=" + System.getProperty("face.native.path"),
                "face.csta-path=" + System.getProperty("face.model.path"), "face.device=SEETA_DEVICE_CPU", "face.max-total=1", "face.max-idle=1")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertTrue(LoadNativeCore.isLoaded());
                for (AutoCloseable proxy : context.getBeansOfType(AutoCloseable.class).values()) {
                    if (!proxy.getClass().getSimpleName().endsWith("Proxy")) continue;
                    String field = proxy instanceof FaceDetectorProxy ? "faceDetectorPool" : "pool";
                    @SuppressWarnings("unchecked")
                    var pool = (org.apache.commons.pool2.impl.GenericObjectPool<Object>)
                        org.springframework.test.util.ReflectionTestUtils.getField(proxy, field);
                    Object handle = pool.borrowObject();
                    handles.add(handle);
                    pool.returnObject(handle);
                }
                assertEquals(16, handles.size());
                var image = SeetafaceUtil.toSeetaImageData(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB));
                assertNotNull(context.getBean(FaceDetectorProxy.class).detect(image));
                assertTrue(context.getBean(FaceRecognizerProxy.class).getExtractFeatureSize() > 0);
            });
        for (Object handle : handles)
            assertEquals(0L, org.springframework.test.util.ReflectionTestUtils.getField(handle, "impl"));
        LoadNativeCore.LOAD_NATIVE(System.getProperty("face.native.path"), SeetaDevice.SEETA_DEVICE_CPU);
    }

    @Test
    void aNativeHandleCanBeClosedTwiceWithoutDoubleDisposal() throws Exception {
        LoadNativeCore.LOAD_NATIVE(System.getProperty("face.native.path"), SeetaDevice.SEETA_DEVICE_CPU);
        String model = Path.of(System.getProperty("face.model.path"), "face_detector.csta").toString();
        var handle = new FaceDetector(new SeetaModelSetting(0, new String[]{model}, SeetaDevice.SEETA_DEVICE_CPU));
        assertNotEquals(0, handle.impl);
        handle.close(); handle.close();
        assertEquals(0, handle.impl);
    }
}
