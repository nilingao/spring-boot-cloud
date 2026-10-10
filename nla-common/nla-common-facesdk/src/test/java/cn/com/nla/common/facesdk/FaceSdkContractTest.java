package cn.com.nla.common.facesdk;

import cn.com.nla.common.facesdk.core.config.FaceSdkAutoConfiguration;
import cn.com.nla.common.facesdk.core.config.FaceSdkNativeLoader;
import cn.com.nla.common.facesdk.core.pool.FaceDetectorPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import cn.com.nla.common.facesdk.core.properties.FaceProperties;
import cn.com.nla.common.facesdk.core.proxy.FaceDetectorProxy;
import cn.com.nla.common.facesdk.core.proxy.FaceRecognizerProxy;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.SeetaDevice;
import com.seeta.sdk.SeetaModelSetting;
import com.seeta.sdk.util.LoadNativeCore;
import com.seeta.sdk.util.SeetafaceUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FaceSdkContractTest {
    @TempDir Path temporary;
    private final ApplicationContextRunner contexts = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FaceSdkAutoConfiguration.class));

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.com.nla.common.facesdk.core")
    static class WideScan { }

    @Test
    void disabledEvenWhenConsumerScansTheCorePackage() {
        try (var loader = mockStatic(LoadNativeCore.class)) {
            contexts.withUserConfiguration(WideScan.class).run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(FaceSdkNativeLoader.class)
                    .doesNotHaveBean(FaceDetectorProxy.class);
                loader.verifyNoInteractions();
            });
        }
    }

    @Test
    void enabledPropertiesBindAndAllSixteenProxyPoolsClose() {
        var loader = mock(FaceSdkNativeLoader.class);
        var proxies = new java.util.ArrayList<Object>();
        contexts.withBean(FaceSdkNativeLoader.class, () -> loader)
            .withPropertyValues("face.enabled=true", "face.dll-path=external-native", "face.csta-path=external-models",
                "face.device=SEETA_DEVICE_CPU", "face.device-id=2", "face.max-total=3", "face.max-idle=2",
                "face.max-wait=2s")
            .run(context -> {
                assertThat(context).hasNotFailed();
                var properties = context.getBean(FaceProperties.class);
                assertEquals(SeetaDevice.SEETA_DEVICE_CPU, properties.getDevice());
                assertEquals(2, properties.getDeviceId());
                assertEquals(Duration.ofSeconds(2), properties.getMaxWait());
                context.getBeansOfType(AutoCloseable.class).values().stream()
                    .filter(bean -> bean.getClass().getSimpleName().endsWith("Proxy")).forEach(proxies::add);
                assertEquals(16, proxies.size());
                for (Object proxy : proxies) {
                    var pool = pool(proxy);
                    assertEquals(3, pool.getMaxTotal()); assertEquals(2, pool.getMaxIdle());
                    assertFalse(pool.isClosed());
                }
            });
        for (Object proxy : proxies) assertTrue(pool(proxy).isClosed());
    }

    private org.apache.commons.pool2.impl.GenericObjectPool<?> pool(Object proxy) {
        String name = proxy instanceof FaceDetectorProxy ? "faceDetectorPool" : "pool";
        return (org.apache.commons.pool2.impl.GenericObjectPool<?>) ReflectionTestUtils.getField(proxy, name);
    }

    @Test
    void aConsumerCanReplaceAnAlgorithmProxy() {
        var detector = mock(FaceDetectorProxy.class);
        contexts.withBean(FaceSdkNativeLoader.class, () -> mock(FaceSdkNativeLoader.class))
            .withBean(FaceDetectorProxy.class, () -> detector)
            .withPropertyValues("face.enabled=true", "face.csta-path=models")
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(FaceDetectorProxy.class));
    }

    @Test
    void invalidEnabledConfigurationFailsStartupBeforeNativeLoading() {
        try (var loader = mockStatic(LoadNativeCore.class)) {
            contexts.withPropertyValues("face.enabled=true").run(context -> assertThat(context).hasFailed());
            loader.verifyNoInteractions();
        }
    }

    @Test
    void missingModelsAndInvalidPoolLimitsAreRejectedBeforeJniCalls() {
        var properties = new FaceProperties(); properties.setCstaPath(temporary.toString());
        assertThat(assertThrows(IllegalArgumentException.class, () -> new FaceSdkNativeLoader(properties).load()))
            .hasMessageContaining("face_detector.csta");
        properties.setMaxTotal(0);
        assertThat(assertThrows(IllegalArgumentException.class, () -> new FaceSdkNativeLoader(properties).load()))
            .hasMessageContaining("pool limits");
    }

    private Path manifest(String platform, String arch, Map<String, String> entries) throws IOException {
        Path directory = temporary.resolve(platform).resolve(arch);
        Files.createDirectories(directory);
        var lines = new java.util.ArrayList<String>();
        for (var entry : entries.entrySet()) lines.add(entry.getKey() + "=" + entry.getValue());
        Files.write(directory.resolve("dll.properties"), lines);
        for (var entry : entries.entrySet()) {
            String file = entry.getValue();
            Path parent = entry.getKey().startsWith("so.base.") ? directory.resolve("base") : directory;
            if (file.contains("tennis")) parent = parent.resolve("CPU");
            Files.createDirectories(parent); Files.write(parent.resolve(file), new byte[]{0});
        }
        return directory;
    }

    @Test
    void nativeManifestUsesNumericBaseThenJniOrderAndAbsoluteWindowsPaths() throws IOException {
        Path directory = manifest("windows", "amd64", Map.of("so.base.10", "last.dll", "so.base.2", "tennis.dll",
            "so.base.0", "authorize.dll", "so.2", "second.dll", "so.1", "first.dll"));
        var paths = LoadNativeCore.resolveLibraries(temporary, "Windows 11", "x86_64", SeetaDevice.SEETA_DEVICE_AUTO);
        assertEquals(List.of("authorize.dll", "tennis.dll", "last.dll", "first.dll", "second.dll"),
            paths.stream().map(path -> path.getFileName().toString()).toList());
        assertEquals(directory.resolve("base/CPU/tennis.dll"), paths.get(1));
        assertTrue(paths.stream().allMatch(Path::isAbsolute));
        assertEquals(paths, LoadNativeCore.resolveLibraries(directory, "Windows 11", "amd64", SeetaDevice.SEETA_DEVICE_CPU));
    }

    @Test
    void linuxArchitectureAliasesAndCpuSelectionAreSupported() throws IOException {
        Path directory = manifest("linux", "aarch64", Map.of("so.base.0", "libtennis.so", "so.0", "libjni.so"));
        assertEquals(directory.resolve("base/CPU/libtennis.so"),
            LoadNativeCore.resolveLibraries(temporary, "Linux", "arm64", SeetaDevice.SEETA_DEVICE_AUTO).getFirst());
        assertThrows(IllegalArgumentException.class,
            () -> LoadNativeCore.resolveLibraries(temporary, "Linux", "aarch64", SeetaDevice.SEETA_DEVICE_GPU));
        assertThrows(IllegalArgumentException.class,
            () -> LoadNativeCore.resolveLibraries(temporary, "Mac OS X", "amd64", SeetaDevice.SEETA_DEVICE_AUTO));
    }

    @Test
    void missingLibraryIsFoundBeforeAnyLoadStarts() throws IOException {
        Path directory = manifest("windows", "amd64", Map.of("so.base.0", "base.dll", "so.0", "jni.dll"));
        Files.delete(directory.resolve("jni.dll"));
        assertThat(assertThrows(IllegalArgumentException.class, () ->
            LoadNativeCore.resolveLibraries(temporary, "Windows", "amd64", SeetaDevice.SEETA_DEVICE_CPU)))
            .hasMessageContaining("jni.dll");
    }

    @Test
    void poolClosesEachOwnedHandleOnceAndRejectsFurtherBorrowing() throws Exception {
        var setting = new SeetaConfSetting<FaceDetector>(new SeetaModelSetting(0, new String[]{"model"}, SeetaDevice.SEETA_DEVICE_CPU));
        try (var construction = mockConstruction(FaceDetector.class, (detector, context) -> {
            detector.impl = 42;
        })) {
            var pool = new FaceDetectorPool(setting);
            var handle = pool.borrowObject(); pool.returnObject(handle);
            pool.close(); pool.close();
            verify(handle, times(1)).close();
            assertThrows(IllegalStateException.class, pool::borrowObject);
        }
    }

    @Test
    void proxyReportsPoolFailureInsteadOfReturningAFalseSuccess() {
        try (var proxy = new FaceRecognizerProxy(new SeetaConfSetting<>())) {
            proxy.close();
            assertThrows(IllegalStateException.class, proxy::getExtractFeatureSize);
            assertEquals(0, pool(proxy).getNumActive());
        }
    }

    @Test
    void imageConversionPreservesBgrChannelsAndProducesDecodablePng() {
        BufferedImage input = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        input.setRGB(0, 0, 0x123456); input.setRGB(1, 0, 0xabcdef);
        var data = SeetafaceUtil.toSeetaImageData(input);
        assertArrayEquals(new byte[]{0x56, 0x34, 0x12, (byte) 0xef, (byte) 0xcd, (byte) 0xab}, data.data);
        var decoded = SeetafaceUtil.toSeetaImageData(new ByteArrayInputStream(SeetafaceUtil.toImageBytes(data, "png")));
        assertArrayEquals(data.data, decoded.data); assertEquals(2, decoded.width);
    }

    @Test
    void unreadableImagesAndUnsupportedFormatsFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> SeetafaceUtil.toSeetaImageData(new ByteArrayInputStream(new byte[]{1, 2})));
        assertThrows(java.io.UncheckedIOException.class, () -> SeetafaceUtil.toBufferedImage(temporary.resolve("missing.png").toFile()));
        assertThrows(IllegalArgumentException.class, () -> SeetafaceUtil.toImageBytes(new com.seeta.sdk.SeetaImageData(1, 1, 3), "unknown"));
    }
}
