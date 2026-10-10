package cn.com.nla.common.facesdk.core.properties;

import com.seeta.sdk.SeetaDevice;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** External SeetaFace installation and per-algorithm pool settings. */
@Data
@ConfigurationProperties("face")
public class FaceProperties {
    private String dllPath;
    private String cstaPath;
    private SeetaDevice device = SeetaDevice.SEETA_DEVICE_AUTO;
    private int deviceId;
    private int maxTotal = 8;
    private int maxIdle = 8;
    private int minIdle;
    private Duration maxWait = Duration.ofSeconds(30);
}
