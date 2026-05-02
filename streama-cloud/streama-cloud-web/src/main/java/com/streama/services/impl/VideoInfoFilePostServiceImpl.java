package com.streama.services.impl;

import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.VideoInfoFilePostQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.mappers.VideoInfoFilePostMapper;
import com.streama.services.VideoInfoFilePostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:视频文件信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
@Service("videoInfoFilePostService")
public class VideoInfoFilePostServiceImpl implements VideoInfoFilePostService{

	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost,VideoInfoFilePostQuery> videoInfoFilePostMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<VideoInfoFilePost> findListByParam(VideoInfoFilePostQuery query) {
		return videoInfoFilePostMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(VideoInfoFilePostQuery query) {
		return videoInfoFilePostMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<VideoInfoFilePost> findListByPage(VideoInfoFilePostQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<VideoInfoFilePost> list = findListByParam(query);
		PaginationResultVO<VideoInfoFilePost> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(VideoInfoFilePost bean) {
		return videoInfoFilePostMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<VideoInfoFilePost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoFilePostMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(VideoInfoFilePost bean) {
		return videoInfoFilePostMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<VideoInfoFilePost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoFilePostMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据FileId查询
	 */
	public VideoInfoFilePost getVideoInfoFilePostByFileId(String fileId) {
		return videoInfoFilePostMapper.selectByFileId(fileId);
	}

	/**
	 * 根据FileId更新
	 */
	public Integer updateVideoInfoFilePostByFileId(VideoInfoFilePost bean, String fileId) {
		return videoInfoFilePostMapper.updateByFileId(bean, fileId);
	}

	/**
	 * 根据FileId删除
	 */
	public Integer deleteVideoInfoFilePostByFileId(String fileId) {
		return videoInfoFilePostMapper.deleteByFileId(fileId);
	}

	/**
	 * 根据UploadIdAndUserId查询
	 */
	public VideoInfoFilePost getVideoInfoFilePostByUploadIdAndUserId(String uploadId, String userId) {
		return videoInfoFilePostMapper.selectByUploadIdAndUserId(uploadId, userId);
	}

	/**
	 * 根据UploadIdAndUserId更新
	 */
	public Integer updateVideoInfoFilePostByUploadIdAndUserId(VideoInfoFilePost bean, String uploadId, String userId) {
		return videoInfoFilePostMapper.updateByUploadIdAndUserId(bean, uploadId, userId);
	}

	/**
	 * 根据UploadIdAndUserId删除
	 */
	public Integer deleteVideoInfoFilePostByUploadIdAndUserId(String uploadId, String userId) {
		return videoInfoFilePostMapper.deleteByUploadIdAndUserId(uploadId, userId);
	}

}
