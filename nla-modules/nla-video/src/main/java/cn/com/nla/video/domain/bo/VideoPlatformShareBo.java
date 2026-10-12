package cn.com.nla.video.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 上级平台级联共享入参：通道/直播流与目录的绑定、解绑与已绑定查询共用。
 *
 * @author TZY
 */
@Data
public class VideoPlatformShareBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 上级平台国标编号
     */
    @NotBlank(message = "上级平台国标编号不能为空")
    private String platformId;

    /**
     * 目录国标编号，等于上级平台国标编号时表示平台根目录
     */
    @NotBlank(message = "目录国标编号不能为空")
    private String catalogId;

    /**
     * 是否包含子目录：0 仅当前目录，1 含全部子目录
     */
    private Integer isSub;

    /**
     * 是否全量处理：0 按 {@code gbIdList} 选择，1 全部
     */
    private Integer isAll;

    /**
     * 通道国标编号或国标流编号集合，{@code isAll} 为 0 时必填
     */
    private List<String> gbIdList;
}
