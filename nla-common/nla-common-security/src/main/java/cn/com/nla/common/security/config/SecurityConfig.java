package cn.com.nla.common.security.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.com.nla.common.core.constant.HttpStatus;
import cn.com.nla.common.core.utils.NetUtils;
import cn.com.nla.common.core.utils.ServletUtils;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.security.config.properties.SecurityProperties;
import cn.com.nla.common.security.handler.AllUrlHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Enumeration;
import java.util.List;

/**
 * 权限安全配置
 *
 * @author TZY
 */

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

    private final SecurityProperties securityProperties;

    /**
     * 注册 Sa-Token 路由拦截器并配置鉴权规则。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
                AllUrlHandler allUrlHandler = SpringUtils.getBean(AllUrlHandler.class);
                // 登录验证 -- 排除多个路径
                SaRouter
                    // 获取所有的
                    .match(allUrlHandler.getUrls())
                    // 对未排除的路径进行检查
                    .check(() -> {
                        HttpServletRequest request = ServletUtils.getRequest();
                        HttpServletResponse response = ServletUtils.getResponse();
                        response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                        // 检查是否登录 是否有token
                        StpUtil.checkLogin();

                        validateClientId(request);
                        validateClientAccessRules(request);

                        // 有效率影响 用于临时测试
                        // if (log.isDebugEnabled()) {
                        //     log.info("剩余有效时间: {}", StpUtil.getTokenTimeout());
                        //     log.info("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
                        // }

                    });
            })).addPathPatterns("/**")
            // 排除不需要拦截的路径
            .excludePathPatterns(securityProperties.getExcludes());
    }

    /**
     * 要求至少提供一个客户端ID，且所有请求头/参数值均与当前 token 一致。
     *
     * @param request 当前请求
     */
    private void validateClientId(HttpServletRequest request) {
        String clientId = getTokenExtra(LoginHelper.CLIENT_KEY);
        if (StringUtils.isBlank(clientId)) {
            throw clientMismatch();
        }
        boolean supplied = false;
        Enumeration<String> headers = request.getHeaders(LoginHelper.CLIENT_KEY);
        while (headers.hasMoreElements()) {
            supplied = true;
            if (!clientId.equals(headers.nextElement())) {
                throw clientMismatch();
            }
        }
        String[] parameters = request.getParameterValues(LoginHelper.CLIENT_KEY);
        if (parameters != null) {
            for (String parameter : parameters) {
                supplied = true;
                if (!clientId.equals(parameter)) {
                    throw clientMismatch();
                }
            }
        }
        if (!supplied) {
            throw clientMismatch();
        }
    }

    /**
     * 将客户端标识缺失或冲突统一转换为既有未登录异常。
     *
     * @return 客户端校验异常
     */
    private NotLoginException clientMismatch() {
        return NotLoginException.newInstance(StpUtil.getLoginType(),
            "-100", "客户端ID与Token不匹配", StpUtil.getTokenValue());
    }

    /**
     * 为 actuator 健康检查接口配置 Basic Auth 鉴权过滤器。
     *
     * @return Sa-Token Servlet 过滤器
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        String username = SpringUtils.getProperty("spring.boot.admin.client.username");
        String password = SpringUtils.getProperty("spring.boot.admin.client.password");
        return new SaServletFilter()
            .addInclude("/actuator", "/actuator/**")
            .setAuth(obj -> {
                SaHttpBasicUtil.check(username + StringUtils.COLON + password);
            })
            .setError(e -> {
                HttpServletResponse response = ServletUtils.getResponse();
                response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                return SaResult.error(e.getMessage()).setCode(HttpStatus.UNAUTHORIZED);
            });
    }

    /**
     * 按客户端配置校验接口访问路径与来源 IP。
     *
     * @param request 当前请求
     */
    private void validateClientAccessRules(HttpServletRequest request) {
        String requestPath = StringUtils.blankToDefault(request.getServletPath(), request.getRequestURI());
        String accessPath = getTokenExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY);
        if (StringUtils.isNotBlank(accessPath)) {
            List<String> accessPathList = StringUtils.str2List(accessPath, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            if (!StringUtils.matches(requestPath, accessPathList)) {
                throw new NotPermissionException("当前客户端未授权访问该接口路径");
            }
        }

        String ipWhitelist = getTokenExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY);
        if (StringUtils.isNotBlank(ipWhitelist)) {
            String clientIp = ServletUtils.getClientIP(request);
            List<String> ipWhitelistList = StringUtils.str2List(ipWhitelist, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            boolean matched = ipWhitelistList.stream().anyMatch(rule -> NetUtils.isMatchIpRule(rule, clientIp));
            if (!matched) {
                throw new NotPermissionException("当前客户端IP不在白名单内");
            }
        }
    }

    /**
     * 读取 token 扩展信息，兼容空值场景。
     *
     * @param key 扩展字段
     * @return 扩展值
     */
    private String getTokenExtra(String key) {
        Object extra = StpUtil.getExtra(key);
        return extra == null ? null : extra.toString();
    }

}
