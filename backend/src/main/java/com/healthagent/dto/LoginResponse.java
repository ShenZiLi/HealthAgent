package com.healthagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 访问令牌
     */
    private String token;

    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private String username;
        private String idCardNo;
        private String realName;

        public UserInfo(String username) {
            this.username = username;
        }
    }
}
