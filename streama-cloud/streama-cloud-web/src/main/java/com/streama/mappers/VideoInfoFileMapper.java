package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * @Description:视频文件信息Mapper
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoFileMapper<T, P> extends BaseMapper {
	/**
	 * 根据FileId查询
	 */
	T selectByFileId(@Param("fileId") String fileId);

	/**
	 * 根据FileId更新
	 */
	Integer updateByFileId(@Param("bean") T bean, @Param("fileId") String fileId);

	/**
	 * 根据FileId删除
	 */
	Integer deleteByFileId(@Param("fileId") String fileId);

	/**
	 * 根据VideoId删除
	 */
	Integer deleteByParams(@Param("query") P query);

}