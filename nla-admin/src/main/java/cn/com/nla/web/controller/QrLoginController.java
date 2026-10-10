package cn.com.nla.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.redis.annotation.RateLimiter;
import cn.com.nla.common.redis.enums.LimitType;
import cn.com.nla.web.domain.model.QrLoginModels.*;
import cn.com.nla.web.service.QrLoginService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/qr")
@RequiredArgsConstructor
public class QrLoginController {
    private final QrLoginService qr;

    @SaIgnore
    @PostMapping("/create")
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    public R<Created> create(@Valid @RequestBody CreateBody body, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.ok(qr.create(body.appid(), body.clientId()));
    }

    @SaIgnore
    @PostMapping("/status")
    @RateLimiter(time = 60, count = 120, limitType = LimitType.IP)
    public R<Status> status(@Valid @RequestBody AccessBody body, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return R.ok(qr.status(body.scene(), body.browserToken(), body.clientId()));
    }

    @PostMapping("/scan")
    public R<ScanTarget> scan(@Valid @RequestBody SceneBody body) {
        StpUtil.checkLogin();
        return R.ok(qr.scan(body.scene()));
    }

    @PostMapping("/confirm")
    public R<Void> confirm(@Valid @RequestBody ConfirmBody body) {
        StpUtil.checkLogin();
        qr.confirm(body.scene(), body.confirmed());
        return R.ok();
    }
}
