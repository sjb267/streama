package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:Mapper
 * @author:孙将斌
 * @date:2026/03/13
 */
public interface UserFocusMapper<T, P> extends BaseMapper {
	/**
	 * 根据UserIdAndFocusUserId查询
	 */
	T selectByUserIdAndFocusUserId(@Param("userId") String userId, @Param("focusUserId") String focusUserId);

	/**
	 * 根据UserIdAndFocusUserId更新
	 */
	Integer updateByUserIdAndFocusUserId(@Param("bean") T bean, @Param("userId") String userId, @Param("focusUserId") String focusUserId);

	/**
	 * 根据UserIdAndFocusUserId删除
	 */
	Integer deleteByUserIdAndFocusUserId(@Param("userId") String userId, @Param("focusUserId") String focusUserId);


	Integer selectFansCount(@Param("userId") String userId);

	Integer selectFocusCount(@Param("userId") String userId);
}