package com.streama.entity.po;


import java.io.Serializable;
import java.util.List;

/**
 * @Description:分类信息
 * @author:孙将斌
 * @date:2026/02/21
 */
public class CategoryInfo implements Serializable {
	/**
	 * 自增分类ID
	 */
	private Integer categoryId;

	/**
	 * 分类编码
	 */
	private String categoryCode;

	/**
	 * 分类名称
	 */
	private String categoryName;

	/**
	 * 父级分类ID
	 */
	private Integer pCategoryId;

	/**
	 * 图标
	 */
	private String icon;

	/**
	 * 排序号
	 */
	private Integer sort;

	private List<CategoryInfo> children;

	public List<CategoryInfo> getChildren() {
		return children;
	}

	public void setChildren(List<CategoryInfo> children) {
		this.children = children;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public Integer getPCategoryId() {
		return pCategoryId;
	}

	public void setPCategoryId(Integer pCategoryId) {
		this.pCategoryId = pCategoryId;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	@Override
	public String toString() {
		return "自增分类ID:" + (categoryId == null ? "空" : categoryId) + ",分类编码:" + (categoryCode == null ? "空" : categoryCode) + ",分类名称:" + (categoryName == null ? "空" : categoryName) + ",父级分类ID:" + (pCategoryId == null ? "空" : pCategoryId) + ",图标:" + (icon == null ? "空" : icon) + ",排序号:" + (sort == null ? "空" : sort);
	}
}