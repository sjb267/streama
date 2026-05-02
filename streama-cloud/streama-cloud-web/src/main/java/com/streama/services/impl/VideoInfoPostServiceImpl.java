package com.streama.services.impl;

import com.streama.component.EsSearchComponent;
import com.streama.component.RedisComponent;
import com.streama.entity.config.AppConfig;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.SysSettingDto;
import com.streama.entity.dto.UploadFileDto;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;
import com.streama.entity.query.*;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.*;
import com.streama.exception.BusinessException;
import com.streama.mappers.*;
import com.streama.services.VideoInfoPostService;
import com.streama.services.AiAuditTaskService;
import com.streama.utils.CopyTools;
import com.streama.utils.FFmpegUtils;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description:视频信息Service
 * @author:孙将斌
 * @date:2026/03/06
 */
@Service("videoInfoPostService")
@Slf4j
public class VideoInfoPostServiceImpl implements VideoInfoPostService{

	@Resource
	private VideoInfoPostMapper<VideoInfoPost,VideoInfoPostQuery> videoInfoPostMapper;

	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;

	@Resource
	private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

	@Resource
	private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;

    @Autowired
    private RedisComponent redisComponent;

//	@Autowired
//	private RabbitComponent rabbitComponent;

	@Resource
	private EsSearchComponent esSearchComponent;

	@Resource
	private AppConfig appConfig;

	@Resource
	private FFmpegUtils ffmpegUtils;

	@Resource
	private UserInfoMapper userInfoMapper;

    @Resource
    private AiAuditTaskService aiAuditTaskService;

	/**
	 * 根据条件查询列表
	 */
	public List<VideoInfoPost> findListByParam(VideoInfoPostQuery query) {
		return videoInfoPostMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(VideoInfoPostQuery query) {
		return videoInfoPostMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<VideoInfoPost> list = findListByParam(query);
		PaginationResultVO<VideoInfoPost> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(VideoInfoPost bean) {
		return videoInfoPostMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoPostMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(VideoInfoPost bean) {
		return videoInfoPostMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return videoInfoPostMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据VideoId查询
	 */
	public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
		return videoInfoPostMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId更新
	 */
	public Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId) {
		return videoInfoPostMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	public Integer deleteVideoInfoPostByVideoId(String videoId) {
		return videoInfoPostMapper.deleteByVideoId(videoId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> uploadFileList) throws BusinessException {
		//1.前置校验
		//分p数量不能超过系统设置
		if(uploadFileList.size() > redisComponent.getSysSettingDto().getVideoPCount()) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//若有videoId，检查视频是否存在
		if(!StringTools.isEmpty(videoInfoPost.getVideoId())) {
			VideoInfoPost videoInfoPostDb = videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
			//传错误videoId
			if(videoInfoPostDb == null) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			//视频正在转码或待审核中，不能编辑
			if(ArrayUtils.contains(new Integer[]{VideoStatusEnum.STATUS0.getStatus(), VideoStatusEnum.STATUS2.getStatus()}, videoInfoPostDb.getStatus())) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}

		Date curDate = new Date();
		String videoId = videoInfoPost.getVideoId();
		List<VideoInfoFilePost> deleteFileList = new ArrayList<>();
		List<VideoInfoFilePost> addFileList = uploadFileList;
		//新增视频（没有videoId)
		if(StringTools.isEmpty(videoId)) {
			videoId = StringTools.getRandomString(Constants.LENGTH_10);
			videoInfoPost.setVideoId(videoId);
			videoInfoPost.setCreateTime(curDate);
			videoInfoPost.setLastUpdateTime(curDate);
			videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
			this.videoInfoPostMapper.insert(videoInfoPost);
		//更新视频信息（有videoId)
		} else {
			//查询数据库中该视频的所有分P文件
			VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
			fileQuery.setVideoId(videoId);
			fileQuery.setUserId(videoInfoPost.getUserId());
			List<VideoInfoFilePost> dbInfoFileList = this.videoInfoFilePostMapper.selectList(fileQuery);
			//以uploadId为key,构造上传文件的Map
			Map<String, VideoInfoFilePost> uploadFileMap = uploadFileList.stream().collect(Collectors.toMap(item -> item.getUploadId(), Function.identity(), (data1, data2) -> data2));

			//找出删除了的文件和改名的文件名（针对前端提交此video后再进行编辑，可以再p文件中进行改名，添加及删除p文件）
			Boolean updateFileName = false;
			for(VideoInfoFilePost fileInfo : dbInfoFileList) {
				VideoInfoFilePost updateFile = uploadFileMap.get(fileInfo.getUploadId());
				//若在上传文件中不存在该分P文件，说明被删除了
				if(updateFile == null) {
					deleteFileList.add(fileInfo);
				//若文件名不同，说明改名了
				} else if(!updateFile.getFileName().equals(fileInfo.getFileName())) {
					updateFileName = true;
				}
			}
			//找出新增的文件（fileId为null则为新增的文件）
			addFileList = uploadFileList.stream().filter(item->item.getFileId() == null).collect(Collectors.toList());
			videoInfoPost.setLastUpdateTime(curDate);
			//检查基本信息是否有变化（标题，封面，标签，简介，投稿类型）
			Boolean changeVideoInfo = this.changeVideoInfo(videoInfoPost);
			if(addFileList != null && !addFileList.isEmpty()) {
				//有新增p则状态改为转码中
				videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
			} else if(changeVideoInfo || updateFileName) {
				//有变化则状态改为待审核
				videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
			}
			this.videoInfoPostMapper.updateByVideoId(videoInfoPost, videoInfoPost.getVideoId());
		}

		//若有删除文件
		if(!deleteFileList.isEmpty()) {
			//将id取出来
			List<String> delFieldList = deleteFileList.stream().map(item -> item.getFileId()).collect(Collectors.toList());
			//删除数据库中的文件记录
			this.videoInfoFilePostMapper.deleteBatchByFileId(delFieldList, videoInfoPost.getUserId());

			//将删除的文件路径取出来
			List<String> delFilePathList = deleteFileList.stream().map(item -> item.getFilePath()).collect(Collectors.toList());

			//将删除的文件路径放入队列中,不要立刻删除，等视频审核完成后再删除
			redisComponent.addFile2DelQueue(videoId, delFilePathList);
		}

		Integer index = 1;
		for(VideoInfoFilePost videoInfoFile : uploadFileList) {
			videoInfoFile.setFileIndex(index++);
			videoInfoFile.setVideoId(videoId);
			videoInfoFile.setUserId(videoInfoPost.getUserId());
			//若没有fileId，说明是新增的文件
			if(videoInfoFile.getFileId() == null) {
				videoInfoFile.setFileId(StringTools.getRandomString(Constants.LENGTH_20));
				videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
				videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
			}
		}
		this.videoInfoFilePostMapper.insertOrUpdateBatch(uploadFileList);

		//若有新增文件,则需要转码
		if(addFileList != null && !addFileList.isEmpty()) {
			for(VideoInfoFilePost file : addFileList) {
				file.setUserId(videoInfoPost.getUserId());
				file.setVideoId(videoId);
			}

			redisComponent.addFile2TransferQueue(addFileList);
		}
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void auditVideo(String videoId, Integer status, String reason) throws BusinessException {
		VideoStatusEnum videoStatusEnum = VideoStatusEnum.getVideoStatusEnum(status);
		if(videoStatusEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		//更新视频审核状态
		VideoInfoPost videoInfoPost = new VideoInfoPost();
		videoInfoPost.setStatus(status);
		VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
		videoInfoPostQuery.setVideoId(videoId);
		videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS2.getStatus());
		Integer auditCount = videoInfoPostMapper.updateByParams(videoInfoPost, videoInfoPostQuery);
		if(auditCount == 0) {
			throw new BusinessException("审核失败, 请稍后重试");
		}

		//更新视频文件审核状态
		VideoInfoFilePost videoInfoFilePost = new VideoInfoFilePost();
		videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.NO_UPDATE.getStatus());
		VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
		filePostQuery.setVideoId(videoId);
		videoInfoFilePostMapper.updateByParams(videoInfoFilePost, filePostQuery);

		//审核不通过则结束
		if(VideoStatusEnum.STATUS4 == videoStatusEnum) {
			return;
		}

		//若是新video则搬入，否则更新
		VideoInfoPost infoPost = videoInfoPostMapper.selectByVideoId(videoId);

		VideoInfo dbVideoInfo = videoInfoMapper.selectByVideoId(videoId);
		if(dbVideoInfo == null) {
			SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
			//给用户加硬币
			userInfoMapper.updateCoinCount(infoPost.getUserId(), sysSettingDto.getPostVideoCoinCount());
		}

		//更新发布信息到视频信息表
		VideoInfo videoInfo = CopyTools.copy(infoPost, VideoInfo.class);
		videoInfoMapper.insertOrUpdate(videoInfo);

		//更新视频信息到正式表，先删除再添加
		VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
		videoInfoFileQuery.setVideoId(videoId);
		videoInfoFileMapper.deleteByParams(videoInfoFileQuery);
		VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
		videoInfoFilePostQuery.setVideoId(videoId);
		List<VideoInfoFilePost> videoInfoFilePosts = videoInfoFilePostMapper.selectList(videoInfoFilePostQuery);

		List<VideoInfoFile> videoInfoFileList = CopyTools.copyList(videoInfoFilePosts, VideoInfoFile.class);
		videoInfoFileMapper.insertBatch(videoInfoFileList);

		//删除(队列需要删除的文件)
		List<String> filePathList = redisComponent.getDelFileList(videoId);
		if(filePathList != null) {
			for(String path:filePathList) {
				File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + path);
				if(file.exists()) {
					try {
						FileUtils.deleteDirectory(file);
					} catch (IOException e) {
						log.error("删除文件失败", e);
					}
				}
			}
		}
		redisComponent.cleanDelFileList(videoId);

		esSearchComponent.saveDoc(videoInfo);
	}

	@Override
	public void transferVideFile4Db(String videoId, String uploadId, String userId, VideoInfoFilePost videoInfoFilePost) {
		videoInfoFilePost.setVideoId(videoId);
		videoInfoFilePost.setUploadId(uploadId);
		videoInfoFilePost.setUserId(userId);
		videoInfoFilePostMapper.updateByUploadIdAndUserId(videoInfoFilePost, uploadId, userId);
		VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
		filePostQuery.setVideoId(videoId);
		filePostQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
		Integer failCount = videoInfoFilePostMapper.selectCount(filePostQuery);
		//检查该video的所有文件是否有转码失败的的
		if(failCount > 0) {
			VideoInfoPost videoUpdate = new VideoInfoPost();
			videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
			videoInfoPostMapper.updateByVideoId(videoUpdate, videoId);
			return;
		}
		filePostQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
		Integer transferCount = videoInfoFilePostMapper.selectCount(filePostQuery);
		//检查video的所有文件是否转码完成(转码失败的已检查）
		if(transferCount == 0) {
			Integer duration = videoInfoFilePostMapper.sumDuration(videoId);
			VideoInfoPost videoUpdate = new VideoInfoPost();
			//进入待审核状态
			videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
			//设置该video所有文件的时间总数
			videoUpdate.setDuration(duration);
			videoInfoPostMapper.updateByVideoId(videoUpdate, videoId);
			//
			aiAuditTaskService.createAuditTaskAndSend(videoId);
		}
	}

	/**
	 * 检查视频基本信息是否有变化（标题，封面，标签，简介, 投稿类型）
	 * @param videoInfoPost
	 * @return
	 * true：有变化，false：无变化
	 */
	private Boolean changeVideoInfo(VideoInfoPost videoInfoPost) {
		VideoInfoPost dbInfo = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
		if(!videoInfoPost.getVideoName().equals(dbInfo.getVideoName())
		 		|| !videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover())
				|| !videoInfoPost.getTags().equals(dbInfo.getTags())
				|| !videoInfoPost.getIntroduction().equals(dbInfo.getIntroduction() == null ? "" : dbInfo.getIntroduction())
				|| !videoInfoPost.getPostType().equals(dbInfo.getPostType())) {
			return true;
		} else {
			return false;
		}
	}

}
