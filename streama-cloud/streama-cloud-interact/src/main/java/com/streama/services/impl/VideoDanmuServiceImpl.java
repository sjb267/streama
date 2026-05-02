package com.streama.services.impl;

import com.streama.api.consumer.VideoClient;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.VideoDanmu;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.VideoDanmuQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.enums.SearchOrderTypeEnum;
import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.VideoDanmuMapper;
import com.streama.services.VideoDanmuService;
import jakarta.annotation.Resource;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:视频弹幕Service
 * @author:孙将斌
 * @date:2026/03/11
 */
@Service("videoDanmuService")
public class VideoDanmuServiceImpl implements VideoDanmuService{

	@Resource
	private VideoDanmuMapper<VideoDanmu,VideoDanmuQuery> videoDanmuMapper;

    @Resource
	private VideoClient videoClient;

	/**
	 * 根据条件查询列表
	 */
	public List<VideoDanmu> findListByParam(VideoDanmuQuery query) {
		return videoDanmuMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(VideoDanmuQuery query) {
		return videoDanmuMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<VideoDanmu> findListByPage(VideoDanmuQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<VideoDanmu> list = findListByParam(query);
		PaginationResultVO<VideoDanmu> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(VideoDanmu bean) {
		return videoDanmuMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<VideoDanmu> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoDanmuMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(VideoDanmu bean) {
		return videoDanmuMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<VideoDanmu> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoDanmuMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据DanmuId查询
	 */
	public VideoDanmu getVideoDanmuByDanmuId(Integer danmuId) {
		return videoDanmuMapper.selectByDanmuId(danmuId);
	}

	/**
	 * 根据DanmuId更新
	 */
	public Integer updateVideoDanmuByDanmuId(VideoDanmu bean, Integer danmuId) {
		return videoDanmuMapper.updateByDanmuId(bean, danmuId);
	}

	/**
	 * 根据DanmuId删除
	 */
	public Integer deleteVideoDanmuByDanmuId(Integer danmuId) {
		return videoDanmuMapper.deleteByDanmuId(danmuId);
	}

	@Override
	public Integer deleteByParam(VideoDanmuQuery query) {
		return videoDanmuMapper.deleteByParams(query);
	}

	@Override
	@GlobalTransactional(rollbackFor = BusinessException.class)
	public void saveVideoDanmu(VideoDanmu videoDanmu) throws BusinessException {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoDanmu.getVideoId());
		if(videoInfo==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(videoInfo.getInteraction()!=null&&videoInfo.getInteraction().contains(Constants.ONE.toString())) {
			throw new BusinessException("博主已关闭弹幕");
		}
		//插入弹幕
		this.videoDanmuMapper.insert(videoDanmu);
		//更新弹幕数
		videoClient.updateCountInfo(videoDanmu.getVideoId(), UserActionTypeEnum.VIDEO_DANMU.getField(), 1);

		videoClient.updateDocCount(videoDanmu.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMU, 1);
	}

	@Override
	public void deleteDanmu(String userId, Integer danmuId) throws BusinessException {
		VideoDanmu videoDanmu = videoDanmuMapper.selectByDanmuId(danmuId);
		if(videoDanmu==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoDanmu.getVideoId());
		if(videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(userId != null && !videoInfo.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		videoDanmuMapper.deleteByDanmuId(danmuId);

		videoClient.updateDocCount(videoDanmu.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMU, -1);
	}
}
