package com.streama.services;

import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.query.VideoInfoFileQuery;
import com.streama.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:视频文件信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
public interface VideoInfoFileService{

	/**
	 * 根据条件查询列表
	 */
	List<VideoInfoFile> findListByParam(VideoInfoFileQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(VideoInfoFileQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<VideoInfoFile> findListByPage(VideoInfoFileQuery query);

	/**
	 * 新增
	 */
	Integer add(VideoInfoFile bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<VideoInfoFile> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(VideoInfoFile bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<VideoInfoFile> listBean);

	/**
	 * 根据FileId查询
	 */
	VideoInfoFile getVideoInfoFileByFileId(String fileId);

	/**
	 * 根据FileId更新
	 */
	Integer updateVideoInfoFileByFileId(VideoInfoFile bean, String fileId);

	/**
	 * 根据FileId删除
	 */
	Integer deleteVideoInfoFileByFileId(String fileId);

}
