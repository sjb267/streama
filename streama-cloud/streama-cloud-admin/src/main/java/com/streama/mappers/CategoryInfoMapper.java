package com.streama.mappers;

import com.streama.entity.po.CategoryInfo;
import com.streama.entity.query.CategoryInfoQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:分类信息Mapper
 * @author:孙将斌
 * @date:2026/02/21
 */
public interface CategoryInfoMapper<T, P> extends BaseMapper {
	/**
	 * 根据CategoryId查询
	 */
	T selectByCategoryId(@Param("categoryId") Integer categoryId);

	/**
	 * 根据CategoryId更新
	 */
	Integer updateByCategoryId(@Param("bean") T bean, @Param("categoryId") Integer categoryId);

	/**
	 * 根据CategoryId删除
	 */
	Integer deleteByCategoryId(@Param("categoryId") Integer categoryId);

	/**
	 * 根据CategoryCode查询
	 */
	T selectByCategoryCode(@Param("categoryCode") String categoryCode);

	/**
	 * 根据CategoryCode更新
	 */
	Integer updateByCategoryCode(@Param("bean") T bean, @Param("categoryCode") String categoryCode);

	/**
	 * 根据CategoryCode删除
	 */
	Integer deleteByCategoryCode(@Param("categoryCode") String categoryCode);

	Integer selectMaxSort(@Param("pCategoryId") Integer pCategoryId);

	Integer deleteByParam(@Param("query") CategoryInfoQuery query);

	void updateSortBatch(@Param("categoryInfoList") List<CategoryInfo> categoryInfoList);
}