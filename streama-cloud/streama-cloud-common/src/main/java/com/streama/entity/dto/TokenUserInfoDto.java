package com.streama.entity.dto;

import java.io.Serial;
import java.io.Serializable;

public class TokenUserInfoDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;
    private String nickName;
    private String avatar;
    private Long expireAt;
    private String token;

    private Integer fansCount;
    private Integer currentCoinCount;
    private Integer focusCount;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getFansCount() {
        return fansCount;
    }

    public void setFansCount(Integer fansCount) {
        this.fansCount = fansCount;
    }

    public Integer getCurrentCoinCount() {
        return currentCoinCount;
    }

    public void setCurrentCoinCount(Integer currentCoinCount) {
        this.currentCoinCount = currentCoinCount;
    }

    public Integer getFocusCount() {
        return focusCount;
    }

    public void setFocusCount(Integer focusCount) {
        this.focusCount = focusCount;
    }
}
