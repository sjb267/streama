package com.streama.services.impl;

import com.streama.api.consumer.InteractClient;
import com.streama.component.EsSearchComponent;
import com.streama.component.RedisComponent;
import com.streama.entity.config.AppConfig;
import com.streama.entity.dto.SysSettingDto;
import com.streama.entity.po.*;
import com.streama.entity.query.*;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.*;
import com.streama.services.VideoInfoFilePostService;
import com.streama.services.VideoInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Description:视频信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
@Service("videoInfoService")
@Slf4j
public class VideoInfoServiceImpl implements VideoInfoService{

	private static ExecutorService executorService = Executors.newFixedThreadPool(10);

	@Resource
	private VideoInfoMapper<VideoInfo,VideoInfoQuery> videoInfoMapper;

	@Resource
	private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private VideoInfoFileMapper videoInfoFileMapper;

	@Resource
	private VideoInfoFilePostMapper videoInfoFilePostMapper;

	@Resource
	private InteractClient interactClient;

    @Resource
    private AppConfig appConfig;

	@Resource
	private EsSearchComponent esSearchComponent;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private VideoInfoFilePostService videoInfoFilePostService;

	/**
	 * 根据条件查询列表
	 */
	public List<VideoInfo> findListByParam(VideoInfoQuery query) {
		return videoInfoMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(VideoInfoQuery query) {
		return videoInfoMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<VideoInfo> findListByPage(VideoInfoQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<VideoInfo> list = findListByParam(query);
		PaginationResultVO<VideoInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(VideoInfo bean) {
		return videoInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<VideoInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(VideoInfo bean) {
		return videoInfoMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<VideoInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据VideoId查询
	 */
	public VideoInfo getVideoInfoByVideoId(String videoId) {
		return videoInfoMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId更新
	 */
	public Integer updateVideoInfoByVideoId(VideoInfo bean, String videoId) {
		return videoInfoMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	public Integer deleteVideoInfoByVideoId(String videoId) {
		return videoInfoMapper.deleteByVideoId(videoId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void changeInteraction(String videoId, String userId, String interaction) {
		VideoInfo videoInfo = new VideoInfo();
		videoInfo.setInteraction(interaction);
		VideoInfoQuery query = new VideoInfoQuery();
		query.setVideoId(videoId);
		query.setUserId(userId);
		videoInfoMapper.updateByParams(videoInfo, query);

		VideoInfoPost videoInfoPost = new VideoInfoPost();
		videoInfoPost.setInteraction(interaction);
		VideoInfoPostQuery query2 = new VideoInfoPostQuery();
		query2.setVideoId(videoId);
		query2.setUserId(userId);
		videoInfoPostMapper.updateByParams(videoInfoPost, query2);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void deleteVideo(String videoId, String userId) throws BusinessException {
		VideoInfo videoInfo = videoInfoMapper.selectByVideoId(videoId);
		//第二种情况是管理员会调用，userId会为null，因此这里过滤传错的userId，若为null直接过
		if(videoInfo == null || userId != null && !userId.equals(videoInfo.getUserId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		videoInfoMapper.deleteByVideoId(videoId);
		videoInfoPostMapper.deleteByVideoId(videoId);

		SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
		userInfoMapper.updateCoinCount(userId, -sysSettingDto.getPostVideoCoinCount());

		esSearchComponent.deleteDoc(videoId);

		executorService.execute(()->{
			VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
			VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
			videoInfoFileQuery.setVideoId(videoId);
			videoInfoQuery.setVideoId(videoId);

			videoInfoFileMapper.deleteByParams(videoInfoFileQuery);

			VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
			videoInfoFilePostQuery.setVideoId(videoId);
			videoInfoFilePostMapper.deleteByParams(videoInfoFilePostQuery);

			interactClient.delCommentByVideoId(videoId);
			interactClient.delDanmuByVideoId(videoId);

			List<VideoInfoFile> videoInfofIleList = videoInfoPostMapper.selectList(videoInfoQuery);
			for(VideoInfoFile item : videoInfofIleList) {
				try {
					FileUtils.deleteDirectory(new File(appConfig.getProjectFolder() + item.getFilePath()));
				} catch (IOException e) {
                    log.error("删除视频文件失败,文件路径:{}", item.getFilePath(), e);
                }
            }
		});
	}

	@Override
	public void addReadCount(String videoId) {
		videoInfoMapper.updateCountInfo(videoId, UserActionTypeEnum.VIDEO_PLAY.getField(), 1);
	}

	@Override
	public void updateCountInfo(String videoId, String field, Integer changeCount) {
		videoInfoMapper.updateCountInfo(videoId, field, changeCount);
	}

}
