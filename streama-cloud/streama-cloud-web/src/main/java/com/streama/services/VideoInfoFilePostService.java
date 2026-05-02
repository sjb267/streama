package com.streama.services;

import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.query.VideoInfoFilePostQuery;
import com.streama.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:视频文件信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoFilePostService{

	/**
	 * 根据条件查询列表
	 */
	List<VideoInfoFilePost> findListByParam(VideoInfoFilePostQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(VideoInfoFilePostQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<VideoInfoFilePost> findListByPage(VideoInfoFilePostQuery query);

	/**
	 * 新增
	 */
	Integer add(VideoInfoFilePost bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<VideoInfoFilePost> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(VideoInfoFilePost bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<VideoInfoFilePost> listBean);

	/**
	 * 根据FileId查询
	 */
	VideoInfoFilePost getVideoInfoFilePostByFileId(String fileId);

	/**
	 * 根据FileId更新
	 */
	Integer updateVideoInfoFilePostByFileId(VideoInfoFilePost bean, String fileId);

	/**
	 * 根据FileId删除
	 */
	Integer deleteVideoInfoFilePostByFileId(String fileId);

	/**
	 * 根据UploadIdAndUserId查询
	 */
	VideoInfoFilePost getVideoInfoFilePostByUploadIdAndUserId(String uploadId, String userId);

	/**
	 * 根据UploadIdAndUserId更新
	 */
	Integer updateVideoInfoFilePostByUploadIdAndUserId(VideoInfoFilePost bean, String uploadId, String userId);

	/**
	 * 根据UploadIdAndUserId删除
	 */
	Integer deleteVideoInfoFilePostByUploadIdAndUserId(String uploadId, String userId);

}
