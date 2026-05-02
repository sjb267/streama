package com.streama.controller;

import com.streama.api.consumer.CategoryClient;
import com.streama.entity.vo.ResponseVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/category")
public class CategoryController extends ABaseController {

    @Resource
    private CategoryClient categoryClient;

    @RequestMapping("/loadAllCategory")
    public ResponseVO loadCategory() {
        return getSuccessResponseVO(categoryClient.loadAllCategory());
    }
}
