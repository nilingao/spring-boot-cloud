package cn.com.nla.web.domain.model;

import cn.com.nla.common.core.domain.model.LoginBody;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class QrLoginBody extends LoginBody {
    @NotBlank @Pattern(regexp = "[0-9a-f]{32}")
    private String scene;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") @ToString.Exclude
    private String browserToken;
}
