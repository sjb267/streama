package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:用户行为Mapper
 * @author:孙将斌
 * @date:2026/03/11
 */
public interface UserActionMapper<T, P> extends BaseMapper {
	/**
	 * 根据ActionId查询
	 */
	T selectByActionId(@Param("actionId") Integer actionId);

	/**
	 * 根据ActionId更新
	 */
	Integer updateByActionId(@Param("bean") T bean, @Param("actionId") Integer actionId);

	/**
	 * 根据ActionId删除
	 */
	Integer deleteByActionId(@Param("actionId") Integer actionId);

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId查询
	 */
	T selectByVideoIdAndCommentIdAndActionTypeAndUserId(@Param("videoId") String videoId, @Param("commentId") Integer commentId, @Param("actionType") Integer actionType, @Param("userId") String userId);

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId更新
	 */
	Integer updateByVideoIdAndCommentIdAndActionTypeAndUserId(@Param("bean") T bean, @Param("videoId") String videoId, @Param("commentId") Integer commentId, @Param("actionType") Integer actionType, @Param("userId") String userId);

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId删除
	 */
	Integer deleteByVideoIdAndCommentIdAndActionTypeAndUserId(@Param("videoId") String videoId, @Param("commentId") Integer commentId, @Param("actionType") Integer actionType, @Param("userId") String userId);

}