package com.streama.services;

import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;
import com.streama.entity.query.VideoInfoPostQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:视频信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoPostService{

	/**
	 * 根据条件查询列表
	 */
	List<VideoInfoPost> findListByParam(VideoInfoPostQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(VideoInfoPostQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery query);

	/**
	 * 新增
	 */
	Integer add(VideoInfoPost bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<VideoInfoPost> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(VideoInfoPost bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<VideoInfoPost> listBean);

	/**
	 * 根据VideoId查询
	 */
	VideoInfoPost getVideoInfoPostByVideoId(String videoId);

	/**
	 * 根据VideoId更新
	 */
	Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId);

	/**
	 * 根据VideoId删除
	 */
	Integer deleteVideoInfoPostByVideoId(String videoId);

	void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> videoInfoFilePostList) throws BusinessException;

	void auditVideo(String videoId, Integer status, String reason) throws BusinessException;

	void transferVideFile4Db(String videoId, String uploadId, String userId, VideoInfoFilePost videoInfoFilePost);
}
