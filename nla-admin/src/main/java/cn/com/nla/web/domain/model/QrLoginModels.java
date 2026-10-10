package cn.com.nla.web.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 网页持有 browserToken；扫码端只提交二维码内的 scene。 */
public final class QrLoginModels {
    private QrLoginModels() { }

    public record CreateBody(@NotBlank @Pattern(regexp = "wx[0-9a-f]{16}") String appid,
                             @NotBlank @Size(max = 128) String clientId) { }

    public record AccessBody(@NotBlank @Pattern(regexp = "[0-9a-f]{32}") String scene,
                             @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String browserToken,
                             @NotBlank @Size(max = 128) String clientId) {
        @Override public String toString() { return "QrAccessBody[scene=" + scene + "]"; }
    }

    public record SceneBody(@NotBlank @Pattern(regexp = "[0-9a-f]{32}") String scene) { }

    public record ConfirmBody(@NotBlank @Pattern(regexp = "[0-9a-f]{32}") String scene,
                              @NotNull Boolean confirmed) { }

    public record Created(String scene, String browserToken, String img, long expireIn) {
        @Override public String toString() { return "QrCreated[scene=" + scene + "]"; }
    }

    public record Status(String status) { }
    public record ScanTarget(String clientId, String clientKey) { }
}
