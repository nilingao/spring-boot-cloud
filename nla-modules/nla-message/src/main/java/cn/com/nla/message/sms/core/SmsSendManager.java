package cn.com.nla.message.sms.core;

import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.sms.supplier.SmsSignUtils;
import cn.com.nla.message.domain.MobileMessage;
import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.domain.bo.SmsSendBo;
import cn.com.nla.message.mapper.MobileMessageMapper;
import cn.com.nla.message.sms.SmsChannelEnum;
import cn.com.nla.message.sms.SmsConstant;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信发送编排器，迁移自旧 {@code SmsSendManager} + {@code AbstractSmsHttpClientManager}。
 * <p>
 * 全流程：防重发拦截 → 选账号（启用且含该消息类型模板，随机 LB）→ 遍历 failover → 渲染模板
 * （变量替换 + 验证码生成）→ 经 {@link SmsFactory} 取对应 {@code SmsBlend} 发送 → 验证码回写 Redis
 * → 落库 {@code sms_mobile_message}。渠道实现下沉在 {@code nla-common-sms}，本类只做业务编排。
 * </p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SmsSendManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    private final SmsChannelManager smsChannelManager;
    private final MobileMessageMapper mobileMessageMapper;

    /**
     * 发送短信（验证码类）。
     * <p>
     * 方法级 {@code synchronized} 保证「防重发检查 → 发送 → 回写」在单实例内串行，与旧实现一致。
     * </p>
     *
     * @param bo 发送参数（消息类型 + 手机号）
     * @return 发送结果
     */
    public synchronized SmsSendResult smsSend(SmsSendBo bo) {
        Integer type = bo.getType();
        String mobile = bo.getMobile();
        String key = SmsConstant.verificationCodeKey(type, mobile);

        // 1.防重发：命中则直接返回剩余等待时间（getTimeToLive 返回毫秒）
        if (RedisUtils.isExistsObject(key)) {
            long ttlMillis = RedisUtils.getTimeToLive(key);
            long seconds = ttlMillis > 0 ? ttlMillis / 1000 : 0;
            long m = seconds / 60 % 60;
            long s = seconds % 60;
            return SmsSendResult.blocked("已发送短信，请于" + m + "分" + s + "秒后重试。", seconds);
        }

        // 2.选账号（已随机打散）；为空表示无可用通道
        List<SmsConfig> accounts = smsChannelManager.findActiveConfigs(type);
        if (accounts.isEmpty()) {
            return SmsSendResult.fail("没有可用的短信接口");
        }

        // 3.遍历 failover：逐个尝试，成功即停；记录最终一次尝试用于落库
        SmsConfig chosen = null;
        Rendered chosenRender = null;
        SmsResponse finalResponse = null;
        boolean success = false;
        for (SmsConfig account : accounts) {
            MobileMessageTemplate template = smsChannelManager.findLastTemplate(account.getId(), type);
            if (template == null) {
                continue;
            }
            Rendered rendered;
            try {
                rendered = render(template);
            } catch (IllegalArgumentException e) {
                log.warn("短信模板变量无效，跳过: configId={}, type={}", account.getId(), type);
                continue;
            }
            if (Integer.valueOf(SmsConstant.TYPE_LOGIN).equals(type)
                && (!type.equals(rendered.templateType) || rendered.redisTime <= 0
                    || StrUtil.isBlank(rendered.verificationCode))) {
                log.warn("登录短信模板缺少有效验证码或缓存期限，跳过: configId={}", account.getId());
                continue;
            }
            SmsResponse response = dispatch(account, mobile, rendered);
            chosen = account;
            chosenRender = rendered;
            finalResponse = response;
            success = response != null && response.isSuccess();
            if (success) {
                break;
            }
            log.warn("短信通道发送失败，尝试下一个: configId={}, smsType={}, resp={}",
                account.getId(), account.getSmsType(), response == null ? "null" : response.getData());
        }

        if (chosen == null) {
            return SmsSendResult.fail("没有可用的短信接口");
        }

        // 4.落库（仅记录最终一次尝试，与旧实现一致）
        record(mobile, chosen, chosenRender, finalResponse, success);

        if (!success) {
            String err = finalResponse == null || finalResponse.getData() == null
                ? "发送失败" : String.valueOf(finalResponse.getData());
            return SmsSendResult.fail(err);
        }

        // 5.验证码回写 Redis（仅登录/注册类型且模板声明了缓存分钟数）
        cacheCode(type, mobile, chosenRender);
        String msgId = finalResponse.getData() == null ? null : String.valueOf(finalResponse.getData());
        Long expireSeconds = chosenRender.redisTime > 0 ? chosenRender.redisTime * 60L : null;
        return SmsSendResult.ok("发送成功", expireSeconds, msgId);
    }

    /**
     * 渲染模板：解析变量 JSON，生成缺失的验证码/缓存分钟数，并做占位替换。
     *
     * @param template 模板
     * @return 渲染结果
     */
    private Rendered render(MobileMessageTemplate template) {
        Rendered r = new Rendered();
        r.content = template.getContent();
        r.rawCode = template.getCode();
        r.templateType = template.getType();
        r.messages = new LinkedHashMap<>();

        String variableJson = template.getVariable();
        if (StrUtil.isBlank(variableJson)) {
            return r;
        }
        LinkedHashMap<String, Object> vars = parseJson(variableJson);

        // 验证码：占位存在且值为空时自动生成 6 位随机码
        if (vars.containsKey(SmsConstant.VERIFICATION_CODE)) {
            Object code = vars.get(SmsConstant.VERIFICATION_CODE);
            if (code == null || StrUtil.isEmpty(String.valueOf(code))) {
                code = CODE_RANDOM.nextInt(900000) + 100000;
                vars.put(SmsConstant.VERIFICATION_CODE, code);
            }
            r.verificationCode = String.valueOf(code);
        }
        // 缓存分钟数：占位存在且值为空时取默认
        if (vars.containsKey(SmsConstant.REDIS_CODE)) {
            Object redisCode = vars.get(SmsConstant.REDIS_CODE);
            if (redisCode == null || StrUtil.isEmpty(String.valueOf(redisCode))) {
                redisCode = SmsConstant.REDIS_TIME;
                vars.put(SmsConstant.REDIS_CODE, redisCode);
            }
            r.redisTime = Integer.parseInt(String.valueOf(redisCode));
        }

        // 占位替换 + 模板参数（供模板型渠道）
        r.content = replaceEach(template.getContent(), vars);
        r.variableJson = writeJson(vars);
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            r.messages.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
        }
        return r;
    }

    /**
     * 按渠道类型分派发送：模板型走「模板编号 + 参数」，内容型走「渲染后内容」。
     *
     * @param account  账号
     * @param mobile   手机号
     * @param rendered 渲染结果
     * @return sms4j 响应；通道未注册或异常返回 {@code null}
     */
    private SmsResponse dispatch(SmsConfig account, String mobile, Rendered rendered) {
        SmsBlend blend = SmsFactory.getSmsBlend(String.valueOf(account.getId()));
        if (blend == null) {
            log.warn("未找到已注册的短信通道: configId={}, smsType={}", account.getId(), account.getSmsType());
            return null;
        }
        SmsChannelEnum channel = SmsChannelEnum.of(account.getSmsType());
        if (channel == null) {
            log.warn("未知或已废弃的短信渠道，跳过: configId={}, smsType={}", account.getId(), account.getSmsType());
            return null;
        }
        try {
            if (channel.isTemplateBased()) {
                // 预解析多渠道模板编号串；对 wyyd 而言其内部再次解析为幂等（无冒号即原样返回）
                String templateId = SmsSignUtils.parseTemplateCode(channel.getSmsType(), rendered.rawCode);
                return blend.sendMessage(mobile, templateId, rendered.messages);
            }
            return blend.sendMessage(mobile, rendered.content);
        } catch (Exception e) {
            log.error("短信通道发送异常: configId={}, smsType={}", account.getId(), account.getSmsType(), e);
            return null;
        }
    }

    /**
     * 落库发送记录（{@code sms_mobile_message}）。审计字段由 {@code BaseEntity} 自动填充，此处仅设业务字段。
     */
    private void record(String mobile, SmsConfig account, Rendered rendered, SmsResponse response, boolean success) {
        MobileMessage msg = new MobileMessage();
        msg.setMobile(mobile);
        msg.setContent(rendered.content);
        msg.setSenderId(account.getId());
        msg.setType(rendered.templateType);
        msg.setVariable(rendered.variableJson);
        // 模板号存原始多渠道编号串，与旧实现一致
        msg.setTemplateId(rendered.rawCode);
        msg.setStatus(success ? SmsConstant.STATUS_SUCCESS : SmsConstant.STATUS_FAIL);
        msg.setHandleTime(LocalDateTime.now());
        if (success && response != null && response.getData() != null) {
            msg.setMsgId(String.valueOf(response.getData()));
        }
        mobileMessageMapper.insert(msg);
    }

    /**
     * 验证码回写：仅登录/注册类型、模板声明了缓存分钟数、且已生成验证码时写入 Redis。
     */
    private void cacheCode(Integer requestType, String mobile, Rendered rendered) {
        Integer templateType = rendered.templateType;
        boolean isCodeType = templateType != null
            && (templateType == SmsConstant.TYPE_LOGIN || templateType == SmsConstant.TYPE_REGISTER);
        if (isCodeType && rendered.redisTime > 0 && StrUtil.isNotBlank(rendered.verificationCode)) {
            String key = SmsConstant.verificationCodeKey(requestType, mobile);
            RedisUtils.setCacheObject(key, rendered.verificationCode, Duration.ofMinutes(rendered.redisTime));
        }
    }

    /**
     * 占位替换：key 按长度降序替换，避免短 key 误伤长 key 的子串（等价旧 {@code StringUtils.replaceEach}）。
     */
    private String replaceEach(String content, Map<String, Object> vars) {
        if (StrUtil.isEmpty(content) || vars.isEmpty()) {
            return content;
        }
        List<String> keys = new ArrayList<>(vars.keySet());
        keys.sort((a, b) -> b.length() - a.length());
        String result = content;
        for (String k : keys) {
            Object v = vars.get(k);
            result = result.replace(k, v == null ? "" : String.valueOf(v));
        }
        return result;
    }

    private LinkedHashMap<String, Object> parseJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("短信模板变量解析失败，按无变量处理: {}", json, e);
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("短信模板变量序列化失败", e);
            return null;
        }
    }

    /**
     * 模板渲染中间结果。
     */
    private static class Rendered {
        /**
         * 渲染后内容
         */
        private String content;
        /**
         * 原始多渠道模板编号串
         */
        private String rawCode;
        /**
         * 消息类型
         */
        private Integer templateType;
        /**
         * 生成的验证码（无则为 null）
         */
        private String verificationCode;
        /**
         * 验证码缓存分钟数（模板未声明则为 0）
         */
        private int redisTime;
        /**
         * 变量重新序列化后的 JSON（落库用）
         */
        private String variableJson;
        /**
         * 模板参数（String→String，供模板型渠道）
         */
        private LinkedHashMap<String, String> messages;
    }
}
