package cn.com.nla.face.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人脸检测信息视图对象，字段语义与旧 FaceVo 对齐。
 *
 * @author TZY
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceDetectVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 年龄估计值。 */
    private Integer age;

    /** 左眼状态：0 闭眼、1 睁眼、2 非眼部区域、3 未知状态。 */
    private Integer leftEyeState;

    /** 右眼状态：0 闭眼、1 睁眼、2 非眼部区域、3 未知状态。 */
    private Integer rightEyeState;

    /** 性别：1 男、2 女。 */
    private Integer gender;

    /** 口罩状态：0 未戴口罩、1 戴了口罩。 */
    private Integer maskStatus;

    /** 人脸特征数组，仅在开启特征提取时填充。 */
    private float[] extract;

    /** 活体状态，取 FaceAntiSpoofing.Status 序号：0 真实人脸、1 攻击、2 成像质量检测中、3 检测中。 */
    private Integer predict;
}
