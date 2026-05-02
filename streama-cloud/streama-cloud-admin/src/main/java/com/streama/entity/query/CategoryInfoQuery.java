package com.streama.entity.query;


/**
 * @Description:分类信息查询对象
 * @author:孙将斌
 * @date:2026/02/21
 */
public class CategoryInfoQuery extends BaseQuery {
	/**
	 * 自增分类ID
	 */
	private Integer categoryId;

	/**
	 * 分类编码
	 */
	private String categoryCode;

	private String categoryCodeFuzzy;

	/**
	 * 分类名称
	 */
	private String categoryName;

	private String categoryNameFuzzy;

	/**
	 * 父级分类ID
	 */
	private Integer pCategoryId;

	/**
	 * 图标
	 */
	private String icon;

	private String iconFuzzy;

	/**
	 * 排序号
	 */
	private Integer sort;

	private Integer categoryIdOrPCategoryId;

	private Boolean convert2Tree;

	public Boolean getConvert2Tree() {
		return convert2Tree;
	}

	public void setConvert2Tree(Boolean convert2Tree) {
		this.convert2Tree = convert2Tree;
	}

	public Integer getCategoryIdOrPCategoryId() {
		return categoryIdOrPCategoryId;
	}

	public void setCategoryIdOrPCategoryId(Integer categoryIdOrPCategoryId) {
		this.categoryIdOrPCategoryId = categoryIdOrPCategoryId;
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

	public String getCategoryCodeFuzzy() {
		return categoryCodeFuzzy;
	}

	public void setCategoryCodeFuzzy(String categoryCodeFuzzy) {
		this.categoryCodeFuzzy = categoryCodeFuzzy;
	}

	public String getCategoryNameFuzzy() {
		return categoryNameFuzzy;
	}

	public void setCategoryNameFuzzy(String categoryNameFuzzy) {
		this.categoryNameFuzzy = categoryNameFuzzy;
	}

	public String getIconFuzzy() {
		return iconFuzzy;
	}

	public void setIconFuzzy(String iconFuzzy) {
		this.iconFuzzy = iconFuzzy;
	}

}