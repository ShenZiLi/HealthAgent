package com.healthagent.controller;

import com.healthagent.common.Result;
import com.healthagent.dto.LoginRequest;
import com.healthagent.dto.LoginResponse;
import com.healthagent.dto.RefreshTokenRequest;
import com.healthagent.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthTokens tokens = authService.login(request.getUsername(), request.getPassword());
        if (tokens != null) {
            LoginResponse.UserInfo userInfo = buildUserInfo(request.getUsername());
            LoginResponse response = new LoginResponse(tokens.accessToken(), tokens.refreshToken(), userInfo);
            return Result.success(response);
        }
        return Result.error("用户名或密码错误");
    }
    
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request != null ? request.getRefreshToken() : null);
        return Result.success();
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return Result.unauthorized("Refresh Token 不能为空");
        }

        AuthService.AuthTokens tokens = authService.refresh(request.getRefreshToken());
        if (tokens == null) {
            return Result.unauthorized("Refresh Token 无效或已过期");
        }

        LoginResponse.UserInfo userInfo = buildUserInfo(tokens.username());
        return Result.success(new LoginResponse(tokens.accessToken(), tokens.refreshToken(), userInfo));
    }
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
    public Result<LoginResponse.UserInfo> getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String username = authService.getUsernameByToken(token);
            if (username != null) {
                return Result.success(buildUserInfo(username));
            }
        }
        return Result.unauthorized("未认证");
    }

    private LoginResponse.UserInfo buildUserInfo(String username) {
        if ("admin".equals(username)) {
            return new LoginResponse.UserInfo(username, "110101199001011234", "张三");
        }
        return new LoginResponse.UserInfo(username);
    }
}
