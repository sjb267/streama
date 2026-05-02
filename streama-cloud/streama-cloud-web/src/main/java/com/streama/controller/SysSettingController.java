package com.streama.controller;

import com.streama.component.RedisComponent;
import com.streama.entity.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysSetting")
@Validated
@Slf4j
public class SysSettingController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/getSetting")
    public ResponseVO getSetting() {
        return getSuccessResponseVO(redisComponent.getSysSettingDto());
    }
}
