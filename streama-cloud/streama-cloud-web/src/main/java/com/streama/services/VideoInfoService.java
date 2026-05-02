package com.streama.services;

import com.streama.entity.po.VideoInfo;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:视频信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoService{

	/**
	 * 根据条件查询列表
	 */
	List<VideoInfo> findListByParam(VideoInfoQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(VideoInfoQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<VideoInfo> findListByPage(VideoInfoQuery query);

	/**
	 * 新增
	 */
	Integer add(VideoInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<VideoInfo> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(VideoInfo bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<VideoInfo> listBean);

	/**
	 * 根据VideoId查询
	 */
	VideoInfo getVideoInfoByVideoId(String videoId);

	/**
	 * 根据VideoId更新
	 */
	Integer updateVideoInfoByVideoId(VideoInfo bean, String videoId);

	/**
	 * 根据VideoId删除
	 */
	Integer deleteVideoInfoByVideoId(String videoId);

	void changeInteraction(String videoId, String userId, String interaction);

	void deleteVideo(String videoId, String userId) throws BusinessException;

	void addReadCount(String videoId);

	void updateCountInfo(String videoId, String field, Integer changeCount);
}
