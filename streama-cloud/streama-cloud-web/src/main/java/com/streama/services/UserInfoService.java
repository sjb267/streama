package com.streama.services;

import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.dto.UserCountInfoDto;
import com.streama.entity.po.UserInfo;
import com.streama.entity.query.UserInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:用户信息
Service
 * @author:孙将斌
 * @date:2026/01/17
 */
public interface UserInfoService{

	/**
	 * 根据条件查询列表
	 */
	List<UserInfo> findListByParam(UserInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(UserInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfo> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(UserInfo bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<UserInfo> listBean);

	/**
	 * 根据UserId查询
	 */
	UserInfo getUserInfoByUserId(String userId);

	/**
	 * 根据UserId更新
	 */
	Integer updateUserInfoByUserId(UserInfo bean, String userId);

	/**
	 * 根据UserId删除
	 */
	Integer deleteUserInfoByUserId(String userId);

	/**
	 * 根据Email查询
	 */
	UserInfo getUserInfoByEmail(String email);

	/**
	 * 根据Email更新
	 */
	Integer updateUserInfoByEmail(UserInfo bean, String email);

	/**
	 * 根据Email删除
	 */
	Integer deleteUserInfoByEmail(String email);

	/**
	 * 根据NickName查询
	 */
	UserInfo getUserInfoByNickName(String nickName);

	/**
	 * 根据NickName更新
	 */
	Integer updateUserInfoByNickName(UserInfo bean, String nickName);

	/**
	 * 根据NickName删除
	 */
	Integer deleteUserInfoByNickName(String nickName);

	void register(String email, String nickName, String registerPassword) throws BusinessException;

	TokenUserInfoDto login(String email, String password, String ip) throws BusinessException;

	UserInfo getUserDetailInfo(String currentUserId,String userId) throws BusinessException;

	void updateUserInfo(UserInfo userInfo, TokenUserInfoDto tokenUserInfoDto) throws BusinessException;

	UserCountInfoDto getUserCountInfo(String userId) throws BusinessException;

	Integer updateCoinCountInfo(String userId, Integer changeCount);
}
