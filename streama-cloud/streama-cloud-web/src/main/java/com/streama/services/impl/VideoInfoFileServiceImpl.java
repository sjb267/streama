package com.streama.services.impl;

import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.VideoInfoFileQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.mappers.VideoInfoFileMapper;
import com.streama.services.VideoInfoFileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:视频文件信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
@Service("videoInfoFileService")
public class VideoInfoFileServiceImpl implements VideoInfoFileService{

	@Resource
	private VideoInfoFileMapper<VideoInfoFile,VideoInfoFileQuery> videoInfoFileMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<VideoInfoFile> findListByParam(VideoInfoFileQuery query) {
		return videoInfoFileMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(VideoInfoFileQuery query) {
		return videoInfoFileMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<VideoInfoFile> findListByPage(VideoInfoFileQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<VideoInfoFile> list = findListByParam(query);
		PaginationResultVO<VideoInfoFile> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(VideoInfoFile bean) {
		return videoInfoFileMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<VideoInfoFile> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoFileMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(VideoInfoFile bean) {
		return videoInfoFileMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<VideoInfoFile> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoFileMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据FileId查询
	 */
	public VideoInfoFile getVideoInfoFileByFileId(String fileId) {
		return videoInfoFileMapper.selectByFileId(fileId);
	}

	/**
	 * 根据FileId更新
	 */
	public Integer updateVideoInfoFileByFileId(VideoInfoFile bean, String fileId) {
		return videoInfoFileMapper.updateByFileId(bean, fileId);
	}

	/**
	 * 根据FileId删除
	 */
	public Integer deleteVideoInfoFileByFileId(String fileId) {
		return videoInfoFileMapper.deleteByFileId(fileId);
	}

}
