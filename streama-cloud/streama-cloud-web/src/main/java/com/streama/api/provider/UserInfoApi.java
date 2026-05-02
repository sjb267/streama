package com.streama.api.provider;

import com.streama.entity.constants.Constants;
import com.streama.entity.po.UserInfo;
import com.streama.services.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/user")
@Validated
public class UserInfoApi {
    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/updateCoinCountInfo")
    public Integer updateCoinCountInfo(@NotEmpty String userId, @NotNull Integer count) {
        return userInfoService.updateCoinCountInfo(userId, count);
    }

    @RequestMapping("/getUserInfoByUserId")
    public UserInfo getUserInfoByUserId(@NotEmpty String userId) {
        return userInfoService.getUserInfoByUserId(userId);
    }
}
