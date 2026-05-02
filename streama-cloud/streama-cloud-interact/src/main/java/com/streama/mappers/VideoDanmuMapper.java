package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:视频弹幕Mapper
 * @author:孙将斌
 * @date:2026/03/11
 */
public interface VideoDanmuMapper<T, P> extends BaseMapper {
	/**
	 * 根据DanmuId查询
	 */
	T selectByDanmuId(@Param("danmuId") Integer danmuId);

	/**
	 * 根据DanmuId更新
	 */
	Integer updateByDanmuId(@Param("bean") T bean, @Param("danmuId") Integer danmuId);

	/**
	 * 根据DanmuId删除
	 */
	Integer deleteByDanmuId(@Param("danmuId") Integer danmuId);

	Integer deleteByParams(@Param("query") P query);
}