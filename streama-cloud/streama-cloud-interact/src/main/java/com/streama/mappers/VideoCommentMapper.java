package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:评论Mapper
 * @author:孙将斌
 * @date:2026/03/11
 */
public interface VideoCommentMapper<T, P> extends BaseMapper {
	/**
	 * 根据CommentId查询
	 */
	T selectByCommentId(@Param("commentId") Integer commentId);

	/**
	 * 根据CommentId更新
	 */
	Integer updateByCommentId(@Param("bean") T bean, @Param("commentId") Integer commentId);

	/**
	 * 根据CommentId删除
	 */
	Integer deleteByCommentId(@Param("commentId") Integer commentId);

	List<T> selectListWithChildren(@Param("query") P p);

	void updateCountInfo(@Param("commentId") Integer commentId, @Param("field") String field, @Param("changeCount") Integer changeCount,
						 @Param("oField") String oField, @Param("oChangeCount") Integer oChangeCount);

	Integer updateByParam(@Param("bean") T bean, @Param("query") P query);

	Integer deleteByParam(@Param("query") P query);
}