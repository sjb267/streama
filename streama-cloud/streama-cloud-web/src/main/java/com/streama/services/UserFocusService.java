package com.streama.services;

import com.streama.entity.po.UserFocus;
import com.streama.entity.query.UserFocusQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:Service
 * @author:孙将斌
 * @date:2026/03/13
 */
public interface UserFocusService{

	/**
	 * 根据条件查询列表
	 */
	List<UserFocus> findListByParam(UserFocusQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserFocusQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserFocus> findListByPage(UserFocusQuery query);

	/**
	 * 新增
	 */
	Integer add(UserFocus bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserFocus> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(UserFocus bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<UserFocus> listBean);

	/**
	 * 根据UserIdAndFocusUserId查询
	 */
	UserFocus getUserFocusByUserIdAndFocusUserId(String userId, String focusUserId);

	/**
	 * 根据UserIdAndFocusUserId更新
	 */
	Integer updateUserFocusByUserIdAndFocusUserId(UserFocus bean, String userId, String focusUserId);

	/**
	 * 根据UserIdAndFocusUserId删除
	 */
	Integer deleteUserFocusByUserIdAndFocusUserId(String userId, String focusUserId);

	void focusUser(String userId, String focusUserId) throws BusinessException;

	void cancelFocusUser(String userId, String focusUserId);

}
