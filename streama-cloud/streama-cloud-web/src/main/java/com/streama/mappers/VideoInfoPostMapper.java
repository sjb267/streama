package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:视频信息Mapper
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoPostMapper<T, P> extends BaseMapper {
	/**
	 * 根据VideoId查询
	 */
	T selectByVideoId(@Param("videoId") String videoId);

	/**
	 * 根据VideoId更新
	 */
	Integer updateByVideoId(@Param("bean") T bean, @Param("videoId") String videoId);

	/**
	 * 根据VideoId删除
	 */
	Integer deleteByVideoId(@Param("videoId") String videoId);

	Integer updateByParams(@Param("bean") T bean, @Param("query") P query);

}