package cn.com.nla.web.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.com.nla.common.core.constant.SystemConstants;
import cn.com.nla.common.core.enums.LoginType;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.exception.user.UserException;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.system.api.model.LoginUser;
import cn.com.nla.system.api.model.XcxLoginUser;
import cn.com.nla.system.domain.vo.SysClientVo;
import cn.com.nla.system.domain.vo.SysUserVo;
import cn.com.nla.system.mapper.SysUserMapper;
import cn.com.nla.system.service.ISysClientService;
import cn.com.nla.system.service.SysXcxBindingService;
import cn.com.nla.web.config.properties.XcxProperties;
import cn.com.nla.web.domain.model.QrLoginModels.*;
import cn.com.nla.web.domain.vo.LoginVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import static cn.com.nla.web.service.QrSceneStore.Phase.*;

/** 小程序显式确认，网页独占领取；所有权限与账号配置在领取时重新读取。 */
@Service
@RequiredArgsConstructor
public class QrLoginService {
    private final XcxProperties properties;
    private final ISysClientService clients;
    private final WechatQrClient images;
    private final QrSceneStore scenes;
    private final SysXcxBindingService bindings;
    private final SysUserMapper users;
    private final SysLoginService loginService;
    private static final SecureRandom RANDOM = new SecureRandom();

    public Created create(String appid, String clientId) {
        client(clientId, "qr");
        var app = app(appid, clientId);
        byte[] sceneBytes = new byte[16]; RANDOM.nextBytes(sceneBytes);
        String scene = HexFormat.of().formatHex(sceneBytes);
        byte[] secret = new byte[32]; RANDOM.nextBytes(secret);
        String browserToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        // 供应商失败不能留下可确认的半成品场景；Redis TTL 从图片生成后开始。
        String image = images.generate(appid, app, scene);
        var state = new QrSceneStore.State(appid, clientId, hash(browserToken), WAITING, null, null);
        if (!scenes.create(scene, state)) { throw unavailable(); }
        return new Created(scene, browserToken, image, 180);
    }

    public Status status(String scene, String browserToken, String clientId) {
        var snapshot = browser(scene, browserToken, clientId);
        return new Status(snapshot.state().phase().name());
    }

    public ScanTarget scan(String scene) {
        var snapshot = read(scene);
        var state = snapshot.state();
        var scanner = scanner(state);
        var target = client(state.clientId(), "qr");
        if (state.phase() == SCANNED && sameScanner(state, scanner)) {
            return new ScanTarget(target.getClientId(), target.getClientKey());
        }
        if (state.phase() != WAITING || !scenes.transition(scene, snapshot,
            state.with(SCANNED, scanner.getUserId(), scanner.getOpenid()))) { throw unavailable(); }
        return new ScanTarget(target.getClientId(), target.getClientKey());
    }

    public void confirm(String scene, boolean confirmed) {
        var snapshot = read(scene);
        var state = snapshot.state();
        var scanner = scanner(state);
        client(state.clientId(), "qr");
        if (state.phase() != SCANNED || !sameScanner(state, scanner)
            || !scenes.transition(scene, snapshot, state.with(confirmed ? CONFIRMED : CANCELLED,
                state.userId(), state.openid()))) { throw unavailable(); }
    }

    public LoginVo redeem(String scene, String browserToken, String clientId) {
        var snapshot = browser(scene, browserToken, clientId);
        var state = snapshot.state();
        if (state.phase() != CONFIRMED) { throw unavailable(); }
        SysClientVo target = client(clientId, "qr");
        if (!Objects.equals(state.userId(), bindings.findUserId(state.appid(), state.openid()))) {
            throw unavailable();
        }
        var user = account(state.userId());
        loginService.checkLogin(LoginType.XCX, user.getUserName(), () -> false);
        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(target.getClientKey());
        loginUser.setDeviceType(target.getDeviceType());
        // 先抢占，再签发；失败后必须重建二维码，不回滚以免重复签发。
        if (!scenes.transition(scene, snapshot, state.with(CONSUMED, state.userId(), state.openid()))) {
            throw unavailable();
        }
        LoginHelper.login(loginUser, IAuthStrategy.buildLoginParameter(target));
        var result = new LoginVo();
        result.setAccessToken(StpUtil.getTokenValue());
        result.setExpireIn(StpUtil.getTokenTimeout());
        result.setClientId(target.getClientId());
        return result;
    }

    private XcxLoginUser scanner(QrSceneStore.State state) {
        StpUtil.checkLogin();
        LoginUser current = LoginHelper.getLoginUser();
        if (!(current instanceof XcxLoginUser mini) || !Objects.equals(state.appid(), mini.getAppid())
            || mini.getUserId() == null || StringUtils.isBlank(mini.getOpenid())) { throw unavailable(); }
        String scannerClient = (String) StpUtil.getExtra(LoginHelper.CLIENT_KEY);
        client(scannerClient, "xcx");
        var app = app(state.appid(), state.clientId());
        if (app.getClientIds() == null || !app.getClientIds().contains(scannerClient)
            || !Objects.equals(mini.getUserId(), bindings.findUserId(mini.getAppid(), mini.getOpenid()))) {
            throw unavailable();
        }
        var user = account(mini.getUserId());
        loginService.checkLogin(LoginType.XCX, user.getUserName(), () -> false);
        return mini;
    }

    private boolean sameScanner(QrSceneStore.State state, XcxLoginUser scanner) {
        return Objects.equals(state.userId(), scanner.getUserId()) && Objects.equals(state.openid(), scanner.getOpenid());
    }

    private QrSceneStore.Snapshot browser(String scene, String token, String clientId) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) { throw unavailable(); }
        var snapshot = read(scene);
        var state = snapshot.state();
        if (!Objects.equals(state.clientId(), clientId) || !MessageDigest.isEqual(
            state.browserHash().getBytes(StandardCharsets.UTF_8), hash(token).getBytes(StandardCharsets.UTF_8))) {
            throw unavailable();
        }
        client(clientId, "qr");
        app(state.appid(), clientId);
        return snapshot;
    }

    private QrSceneStore.Snapshot read(String scene) {
        if (scene == null || !scene.matches("[0-9a-f]{32}")) { throw unavailable(); }
        var snapshot = scenes.read(scene);
        if (snapshot == null || snapshot.state() == null) { throw unavailable(); }
        return snapshot;
    }

    private SysClientVo client(String clientId, String grant) {
        if (StringUtils.isBlank(clientId)) { throw unavailable(); }
        var client = clients.queryByClientId(clientId);
        if (client == null || !clientId.equals(client.getClientId()) || !SystemConstants.NORMAL.equals(client.getStatus())
            || !StringUtils.str2List(client.getGrantType(), ",", true, true).contains(grant)) { throw unavailable(); }
        return client;
    }

    private XcxProperties.App app(String appid, String clientId) {
        var app = properties.getApps() == null ? null : properties.getApps().get(appid);
        if (appid == null || !appid.matches("wx[0-9a-f]{16}") || !properties.isEnabled() || !properties.isQrEnabled()
            || app == null || !app.isEnabled() || StringUtils.isBlank(app.getSecret())
            || app.getQrClientIds() == null || !app.getQrClientIds().contains(clientId)
            || app.getQrPage() == null || !app.getQrPage().matches("[A-Za-z0-9_/-]{1,128}")
            || app.getQrPage().startsWith("/") || app.getQrPage().contains("//")
            || app.getQrEnvVersion() == null || !List.of("release", "trial", "develop").contains(app.getQrEnvVersion())) { throw unavailable(); }
        return app;
    }

    private SysUserVo account(Long userId) {
        var user = userId == null ? null : users.selectVoById(userId);
        if (user == null) { throw new UserException("user.not.exists", userId); }
        if (!SystemConstants.NORMAL.equals(user.getStatus())) { throw new UserException("user.blocked", userId); }
        return user;
    }

    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static ServiceException unavailable() { return new ServiceException("二维码不可用或已过期，请重新获取"); }
}
