package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:用户信息
Mapper
 * @author:孙将斌
 * @date:2026/01/17
 */
public interface UserInfoMapper<T, P> extends BaseMapper {
	/**
	 * 根据UserId查询
	 */
	T selectByUserId(@Param("userId") String userId);

	/**
	 * 根据UserId更新
	 */
	Integer updateByUserId(@Param("bean") T bean, @Param("userId") String userId);

	/**
	 * 根据UserId删除
	 */
	Integer deleteByUserId(@Param("userId") String userId);

	/**
	 * 根据Email查询
	 */
	T selectByEmail(@Param("email") String email);

	/**
	 * 根据Email更新
	 */
	Integer updateByEmail(@Param("bean") T bean, @Param("email") String email);

	/**
	 * 根据Email删除
	 */
	Integer deleteByEmail(@Param("email") String email);

	/**
	 * 根据NickName查询
	 */
	T selectByNickName(@Param("nickName") String nickName);

	/**
	 * 根据NickName更新
	 */
	Integer updateByNickName(@Param("bean") T bean, @Param("nickName") String nickName);

	/**
	 * 根据NickName删除
	 */
	Integer deleteByNickName(@Param("nickName") String nickName);
;
	Integer updateCoinCount(@Param("userId") String userId, @Param("changeCount") Integer changeCount);
}