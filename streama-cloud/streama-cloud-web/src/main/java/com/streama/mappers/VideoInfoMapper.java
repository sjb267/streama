package com.streama.mappers;

import com.streama.entity.dto.CountInfoDto;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:视频信息Mapper
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoMapper<T, P> extends BaseMapper {
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

	void updateCountInfo(@Param("videoId") String videoId, @Param("field") String field, @Param("changeCount") Integer changeCount);

	Integer updateByParams(@Param("bean") T bean, @Param("query") P query);

	CountInfoDto selectSumCountInfo(@Param("userId")String userId);
}