package com.streama.services;

import com.streama.entity.po.CategoryInfo;
import com.streama.entity.query.CategoryInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:分类信息Service
 * @author:孙将斌
 * @date:2026/02/21
 */
public interface CategoryInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<CategoryInfo> findListByParam(CategoryInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(CategoryInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<CategoryInfo> findListByPage(CategoryInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(CategoryInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<CategoryInfo> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(CategoryInfo bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<CategoryInfo> listBean);

	/**
	 * 根据CategoryId查询
	 */
	CategoryInfo getCategoryInfoByCategoryId(Integer categoryId);

	/**
	 * 根据CategoryId更新
	 */
	Integer updateCategoryInfoByCategoryId(CategoryInfo bean, Integer categoryId);

	/**
	 * 根据CategoryId删除
	 */
	Integer deleteCategoryInfoByCategoryId(Integer categoryId);

	/**
	 * 根据CategoryCode查询
	 */
	CategoryInfo getCategoryInfoByCategoryCode(String categoryCode);

	/**
	 * 根据CategoryCode更新
	 */
	Integer updateCategoryInfoByCategoryCode(CategoryInfo bean, String categoryCode);

	/**
	 * 根据CategoryCode删除
	 */
	Integer deleteCategoryInfoByCategoryCode(String categoryCode);

	/**
	 * 新增或更新分类信息
	 * @param bean
	 * @throws BusinessException
	 */
	void saveCategory(CategoryInfo bean) throws BusinessException;

	void delCategory(Integer categoryId) throws BusinessException;

	void changeSort(Integer categoryId, String categoryIds);

	public List<CategoryInfo> getAllCategoryList();
}
