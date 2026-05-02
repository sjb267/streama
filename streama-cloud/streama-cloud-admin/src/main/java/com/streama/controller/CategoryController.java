package com.streama.controller;

import com.streama.entity.po.CategoryInfo;
import com.streama.entity.query.CategoryInfoQuery;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.CategoryInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController extends ABaseController {

    @Resource
    private CategoryInfoService categoryInfoService;

    @RequestMapping("/loadCategory")
    public ResponseVO loadCategory(CategoryInfoQuery query) {
        query.setOrderBy("sort asc");
        query.setConvert2Tree(true);
        List<CategoryInfo> categoryInfoList = categoryInfoService.findListByParam(query);
        return getSuccessResponseVO(categoryInfoList);
    }

    @RequestMapping("saveCategory")
    public ResponseVO saveCategory(@NotNull Integer pCategoryId,
                                   Integer categoryId,
                                   @NotEmpty String categoryCode,
                                   @NotEmpty String categoryName,
                                   String icon) throws BusinessException {

        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setPCategoryId(pCategoryId);
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setCategoryCode(categoryCode);
        categoryInfo.setCategoryName(categoryName);
        categoryInfo.setIcon(icon);

        categoryInfoService.saveCategory(categoryInfo);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("delCategory")
    public ResponseVO delCategory(@NotNull Integer categoryId) throws BusinessException {
        categoryInfoService.delCategory(categoryId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("changeSort")
    public ResponseVO changeSort(@NotNull Integer pCategoryId,
                                 @NotEmpty String categoryIds) throws BusinessException {
        categoryInfoService.changeSort(pCategoryId, categoryIds);
        return getSuccessResponseVO(null);
    }
}