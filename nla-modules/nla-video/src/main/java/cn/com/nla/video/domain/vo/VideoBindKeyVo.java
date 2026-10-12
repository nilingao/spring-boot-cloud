package cn.com.nla.video.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 级联目录已绑定编号集合，供前端目录绑定的复选框回显。
 *
 * <p>编号集合同时包含通道/流国标编号与其所属设备国标编号，便于树形控件按设备节点勾选。</p>
 *
 * @author TZY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoBindKeyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 该上级平台已绑定的全部编号
     */
    private List<String> allGbIdList;

    /**
     * 当前目录（含所选子目录）下已绑定的编号
     */
    private List<String> useCatalogGbIdList;
}
