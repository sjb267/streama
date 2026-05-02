package com.streama.services.impl;

import com.streama.api.consumer.WebClient;
import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.CategoryInfo;
import com.streama.entity.query.CategoryInfoQuery;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.exception.BusinessException;
import com.streama.mappers.CategoryInfoMapper;
import com.streama.services.CategoryInfoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:分类信息Service
 * @author:孙将斌
 * @date:2026/02/21
 */
@Service("categoryInfoService")
public class CategoryInfoServiceImpl implements CategoryInfoService{

	@Resource
	private CategoryInfoMapper<CategoryInfo,CategoryInfoQuery> categoryInfoMapper;

	@Resource
	private RedisComponent redisComponent;

    @Resource
	private WebClient webClient;

	/**
	 * 根据条件查询列表
	 */
	public List<CategoryInfo> findListByParam(CategoryInfoQuery query) {
		List<CategoryInfo> categoryInfoList = categoryInfoMapper.selectList(query);
		if(query.getConvert2Tree()!=null && query.getConvert2Tree()){
			categoryInfoList = convertLine2Tree(categoryInfoList, Constants.ZERO);
		}
		return categoryInfoList;
	}

	private List<CategoryInfo> convertLine2Tree(List<CategoryInfo> dataList, Integer pid) {
		List<CategoryInfo> children = new ArrayList<>();
		for(CategoryInfo m : dataList){
			if(m.getCategoryId() != null && m.getPCategoryId() != null && m.getPCategoryId().equals(pid)){
				m.setChildren(convertLine2Tree(dataList, m.getCategoryId()));
				children.add(m);
			}
		}
		return children;
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(CategoryInfoQuery query) {
		return categoryInfoMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<CategoryInfo> findListByPage(CategoryInfoQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<CategoryInfo> list = findListByParam(query);
		PaginationResultVO<CategoryInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(CategoryInfo bean) {
		return categoryInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return categoryInfoMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(CategoryInfo bean) {
		return categoryInfoMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return categoryInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据CategoryId查询
	 */
	public CategoryInfo getCategoryInfoByCategoryId(Integer categoryId) {
		return categoryInfoMapper.selectByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryId更新
	 */
	public Integer updateCategoryInfoByCategoryId(CategoryInfo bean, Integer categoryId) {
		return categoryInfoMapper.updateByCategoryId(bean, categoryId);
	}

	/**
	 * 根据CategoryId删除
	 */
	public Integer deleteCategoryInfoByCategoryId(Integer categoryId) {
		return categoryInfoMapper.deleteByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryCode查询
	 */
	public CategoryInfo getCategoryInfoByCategoryCode(String categoryCode) {
		return categoryInfoMapper.selectByCategoryCode(categoryCode);
	}

	/**
	 * 根据CategoryCode更新
	 */
	public Integer updateCategoryInfoByCategoryCode(CategoryInfo bean, String categoryCode) {
		return categoryInfoMapper.updateByCategoryCode(bean, categoryCode);
	}

	/**
	 * 根据CategoryCode删除
	 */
	public Integer deleteCategoryInfoByCategoryCode(String categoryCode) {
		return categoryInfoMapper.deleteByCategoryCode(categoryCode);
	}

	@Override
	public void saveCategory(CategoryInfo bean) throws BusinessException {
		CategoryInfo dbBean = this.categoryInfoMapper.selectByCategoryCode(bean.getCategoryCode());
		if (bean.getCategoryId() == null && dbBean != null ||
			bean.getCategoryId() != null && dbBean != null && !bean.getCategoryId().equals(dbBean.getCategoryId())){
			throw new BusinessException("分类编码已存在");
		}
		if(bean.getCategoryId() == null){
			Integer maxSort = this.categoryInfoMapper.selectMaxSort(bean.getPCategoryId());
			bean.setSort(maxSort + 1);
			this.categoryInfoMapper.insert(bean);
		}else{
			this.categoryInfoMapper.updateByCategoryId(bean, bean.getCategoryId());
		}

		save2Cache();
	}

	@Override
	public void delCategory(Integer categoryId) throws BusinessException {
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setCategoryIdOrPCategoryId(categoryId);
		//TODO WEB服务提供分类
		Integer count = webClient.getVideoCount(videoInfoQuery);
		if(count > 0) {
			throw new BusinessException("分类下有视频，不能删除");
		}
		CategoryInfoQuery categoryInfoQuery = new CategoryInfoQuery();
		categoryInfoQuery.setCategoryIdOrPCategoryId(categoryId);
		categoryInfoMapper.deleteByParam(categoryInfoQuery);

		save2Cache();
	}

	@Override
	public void changeSort(Integer pCategoryId, String categoryIds) {
		String[] categoryIdArray = categoryIds.split(",");
		List<CategoryInfo> categoryInfoList = new ArrayList<>();
		Integer sort = 1;
		for(String categoryId: categoryIdArray) {
			CategoryInfo categoryInfo = new CategoryInfo();
			categoryInfo.setCategoryId(Integer.parseInt(categoryId));
			categoryInfo.setPCategoryId(pCategoryId);
			categoryInfo.setSort(++sort);
			categoryInfoList.add(categoryInfo);
		}
		categoryInfoMapper.updateSortBatch(categoryInfoList);
		save2Cache();
	}

	@Override
	public List<CategoryInfo> getAllCategoryList() {
		List<CategoryInfo> categoryInfoList = redisComponent.getCategoryInfoList();
		if(categoryInfoList == null || categoryInfoList.isEmpty()){
			save2Cache();
		}
		return redisComponent.getCategoryInfoList();
	}

	private void save2Cache() {
		CategoryInfoQuery categoryInfoQuery = new CategoryInfoQuery();
		categoryInfoQuery.setOrderBy("sort asc");
		categoryInfoQuery.setConvert2Tree(true);
		List<CategoryInfo> categoryInfoList = findListByParam(categoryInfoQuery);
		redisComponent.saveCategoryInfoList(categoryInfoList);
	}
}
