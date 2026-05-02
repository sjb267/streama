package com.streama.services.impl;

import com.streama.entity.po.UserFocus;
import com.streama.entity.po.UserInfo;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.UserFocusQuery;
import com.streama.entity.query.UserInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.UserFocusMapper;
import com.streama.mappers.UserInfoMapper;
import com.streama.services.UserFocusService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @Description:Service
 * @author:孙将斌
 * @date:2026/03/13
 */
@Service("userFocusService")
public class UserFocusServiceImpl implements UserFocusService{

	@Resource
	private UserFocusMapper<UserFocus,UserFocusQuery> userFocusMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<UserFocus> findListByParam(UserFocusQuery query) {
		return userFocusMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserFocusQuery query) {
		return userFocusMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserFocus> findListByPage(UserFocusQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserFocus> list = findListByParam(query);
		PaginationResultVO<UserFocus> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(UserFocus bean) {
		return userFocusMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserFocus> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return userFocusMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(UserFocus bean) {
		return userFocusMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<UserFocus> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return userFocusMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据UserIdAndFocusUserId查询
	 */
	public UserFocus getUserFocusByUserIdAndFocusUserId(String userId, String focusUserId) {
		return userFocusMapper.selectByUserIdAndFocusUserId(userId, focusUserId);
	}

	/**
	 * 根据UserIdAndFocusUserId更新
	 */
	public Integer updateUserFocusByUserIdAndFocusUserId(UserFocus bean, String userId, String focusUserId) {
		return userFocusMapper.updateByUserIdAndFocusUserId(bean, userId, focusUserId);
	}

	/**
	 * 根据UserIdAndFocusUserId删除
	 */
	public Integer deleteUserFocusByUserIdAndFocusUserId(String userId, String focusUserId) {
		return userFocusMapper.deleteByUserIdAndFocusUserId(userId, focusUserId);
	}

	@Override
	public void focusUser(String userId, String focusUserId) throws BusinessException {
		if(userId.equals(focusUserId)) {
			throw new BusinessException("不能关注自己");
		}
		UserFocus dbInfo = userFocusMapper.selectByUserIdAndFocusUserId(userId, focusUserId);
		if(dbInfo != null) {
			return;
		}
		UserInfo userInfo = userInfoMapper.selectByUserId(focusUserId);
		if(userInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserFocus focus = new UserFocus();
		focus.setUserId(userId);
		focus.setFocusUserId(focusUserId);
		focus.setFocusTime(new Date());
		userFocusMapper.insert(focus);
	}

	@Override
	public void cancelFocusUser(String userId, String focusUserId) {
		userFocusMapper.deleteByUserIdAndFocusUserId(userId, focusUserId);
	}

}
