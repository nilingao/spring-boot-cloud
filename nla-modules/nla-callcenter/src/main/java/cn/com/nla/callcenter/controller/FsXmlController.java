package cn.com.nla.callcenter.controller;

import cn.com.nla.callcenter.spi.AgentService;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.enums.fs.FsTypeEnum;
import cn.com.nla.common.freeswitch.model.bean.ConfigModel;
import cn.com.nla.common.freeswitch.model.bean.UserModel;
import cn.com.nla.common.freeswitch.utils.FreeswitchUtils;
import cn.com.nla.common.freeswitch.vo.fs.FreeswitchXmlVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * FreeSWITCH XML 配置拉取接口。
 *
 * <p>迁移自旧 controller.api.fs.FsXmlController：供 FreeSWITCH 服务器经
 * HTTP POST 回拉拨号计划/用户目录/配置项 XML，路径由旧 /api/fs/xml 调整为
 * /fs/xml（匿名放行见 security.excludes，FS 服务器侧无登录态）。</p>
 *
 * <p>仅在 freeswitch.enabled=true 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/fs/xml")
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class FsXmlController {

    private final AgentService agentService;

    /**
     * 获取拨号计划的 xml 信息。
     *
     * @param req 请求
     * @return 拨号计划 XML，未配置时返回 null
     */
    @PostMapping("/find_dialplan_xml")
    public String findDialplanXml(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        String xml = FreeswitchUtils.getXmlConfig(FreeswitchXmlVo.builder()
                .fsTypeEnum(FsTypeEnum.DIALPLAN)
                .build()
        );
        log.warn("请求Fs拨号计划文件： 是否配置Xml：{}，请求参数：{}", StringUtils.isNotBlank(xml), parameterMap);
        return xml;
    }

    /**
     * 获取呼叫账户的 xml 信息。
     *
     * @param req 请求（domain/user/key 参数）
     * @return 用户目录 XML，未找到坐席时返回未授权目录 XML
     */
    @PostMapping("/find_directory_xml")
    public String getDirectoryXML(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        String domain = req.getParameter("domain");
        String sip = req.getParameter("user");
        String key = req.getParameter("key");
        String xml;
        UserModel userModel = agentService.findUserModel(sip);
        if (userModel == null) {
            xml = FreeswitchUtils.getXmlConfig(FreeswitchXmlVo.builder()
                    .fsTypeEnum(FsTypeEnum.NOT_FIND)
                    .modelMap(Map.of("type", "directory"))
                    .build()
            );
        } else {
            userModel.setDomain(domain);
            xml = FreeswitchUtils.getXmlConfig(FreeswitchXmlVo.builder()
                    .fsTypeEnum(FsTypeEnum.USER)
                    .modelMap(Map.of(FsTypeEnum.USER.getName(), userModel))
                    .build()
            );
        }
        log.warn("请求Fs呼叫用户文件key：{} user:{} 是否配置Xml：{}，请求参数：{}", key, sip, StringUtils.isNotBlank(xml), parameterMap);
        return xml;
    }

    /**
     * 获取配置项的 xml 信息。
     *
     * @param req 请求（key_value/profile 参数）
     * @return 配置项 XML，未匹配的配置返回 null
     */
    @PostMapping("/find_configuration_xml")
    public String findConfigurationXml(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        String key = req.getParameter("key_value");
        String xml = null;
        if (key != null) {
            switch (key) {
                case "sofia.conf" -> {
                    if ("internal".equals(req.getParameter("profile"))) {
                        xml = FreeswitchUtils.getXmlConfig(FreeswitchXmlVo.builder()
                                .fsTypeEnum(FsTypeEnum.INTERNAL)
                                .modelMap(Map.of(FsTypeEnum.INTERNAL.getName(), ConfigModel.builder()
                                        .iceStart(ConstEnum.Flag.YES.getValue())
                                        .stunAddress("autonat:1.82.217.118")
                                        .build()))
                                .build()
                        );
                    }
                }
                case "post_load_switch.conf" -> xml = FreeswitchUtils.getXmlConfig(FreeswitchXmlVo.builder()
                        .fsTypeEnum(FsTypeEnum.POST_LOAD_SWITCH)
                        .modelMap(Map.of(FsTypeEnum.POST_LOAD_SWITCH.getName(), ConfigModel.builder()
                                .startRtpPort("16384")
                                .endRtpPort("16484")
                                .build()))
                        .build()
                );
                case "conference.conf" -> xml = FreeswitchUtils.getXmlConfig(FreeswitchXmlVo.builder()
                        .fsTypeEnum(FsTypeEnum.CONFERENCE)
                        .modelMap(Map.of(FsTypeEnum.CONFERENCE.getName(), Constant.SOUND_PREFIX))
                        .build()
                );
                default -> {
                }
            }
        }
        log.warn("请求Fs配置文件：{}，是否配置Xml：{}，请求参数：{}", key, StringUtils.isNotBlank(xml), parameterMap);
        return xml;
    }
}
