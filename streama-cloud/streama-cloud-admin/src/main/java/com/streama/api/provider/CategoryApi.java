package com.streama.api.provider;

import com.streama.entity.constants.Constants;
import com.streama.entity.po.CategoryInfo;
import com.streama.services.CategoryInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX)
public class CategoryApi {

    @Resource
    private CategoryInfoService categoryInfoService;

    @RequestMapping("/loadAllCategory")
    public List<CategoryInfo> loadAllCategory() {
        return categoryInfoService.getAllCategoryList();
    }
}
