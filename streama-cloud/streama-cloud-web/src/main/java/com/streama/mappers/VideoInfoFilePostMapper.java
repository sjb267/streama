package com.streama.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:视频文件信息Mapper
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoFilePostMapper<T, P> extends BaseMapper {
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
	 * 根据UploadIdAndUserId查询
	 */
	T selectByUploadIdAndUserId(@Param("uploadId") String uploadId, @Param("userId") String userId);

	/**
	 * 根据UploadIdAndUserId更新
	 */
	Integer updateByUploadIdAndUserId(@Param("bean") T bean, @Param("uploadId") String uploadId, @Param("userId") String userId);

	/**
	 * 根据UploadIdAndUserId删除
	 */
	Integer deleteByUploadIdAndUserId(@Param("uploadId") String uploadId, @Param("userId") String userId);

	void deleteBatchByFileId(@Param("fileIdList") List<String> fileIdList, @Param("userId") String userId);

	Integer sumDuration(@Param("videoId") String videoId);

	Integer updateByParams(@Param("bean") T bean, @Param("query") P query);

	Integer deleteByParams(@Param("query") P query);
}