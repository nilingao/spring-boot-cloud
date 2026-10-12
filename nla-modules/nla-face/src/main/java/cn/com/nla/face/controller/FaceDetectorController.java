package cn.com.nla.face.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.face.domain.vo.FaceDetectVo;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.service.IFaceDetectorService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 人脸识别通用端点：检测、1:1 比对与 1:N 检索。
 * 定位为登录用户可用的基础能力接口（如头像上传前活体校验），不做按钮级权限拆分。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/face/detector")
public class FaceDetectorController extends BaseController {

    private final IFaceDetectorService detectorService;

    /**
     * 全属性人脸检测（活体、性别、年龄、双眼、口罩）
     *
     * @param file 人脸图片
     * @return 检测信息
     */
    @SaCheckLogin
    @PostMapping("/analyze")
    public R<FaceDetectVo> analyze(@RequestPart("file") MultipartFile file) {
        return R.ok(detectorService.findFaceInfo(file));
    }

    /**
     * 两张图片 1:1 人脸相似度比对
     *
     * @param file1 图片一
     * @param file2 图片二
     * @return 相似度 0~1
     */
    @SaCheckLogin
    @PostMapping("/comparison")
    public R<Float> comparison(@RequestPart("file1") MultipartFile file1,
                               @RequestPart("file2") MultipartFile file2) {
        return R.ok(detectorService.comparison(file1, file2));
    }

    /**
     * 1:N 人脸检索
     *
     * @param file      待检索图片
     * @param size      返回条数上限，默认 1
     * @param calculate 相似度百分比阈值，默认 70
     * @return 命中人员列表
     */
    @SaCheckLogin
    @PostMapping("/search")
    public R<List<PersonVo>> search(@RequestPart("file") MultipartFile file,
                                    Integer size, Integer calculate) {
        return R.ok(detectorService.search(file, size, calculate));
    }
}
