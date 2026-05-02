package com.streama.controller;

import com.streama.api.consumer.InteractClient;
import com.streama.component.EsSearchComponent;
import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.po.UserAction;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.query.UserActionQuery;
import com.streama.entity.query.VideoInfoFileQuery;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.entity.vo.VideoInfoResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.SearchOrderTypeEnum;
import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.entity.enums.VideoRecommendTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.services.impl.VideoInfoFileServiceImpl;
import com.streama.services.VideoInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/video")
@Validated
public class VideoController extends ABaseController {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileServiceImpl videoInfoFileService;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private InteractClient interactClient;

    @RequestMapping("/loadRecommendVideoList")
    public ResponseVO loadRecommendVideo() {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        // 查询用户信息
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("create_time desc");
        videoInfoQuery.setRecommendType(VideoRecommendTypeEnum.RECOMMEND.getType());
        List<VideoInfo> recommendVideoList = videoInfoService.findListByParam(videoInfoQuery);
        return getSuccessResponseVO(recommendVideoList);
    }

    @RequestMapping("/loadVideoList")
    public ResponseVO loadVideoList(Integer pCategoryId, Integer categoryId, Integer pageNo, Integer pageSize) {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setPCategoryId(pCategoryId);
        videoInfoQuery.setCategoryId(categoryId);
        videoInfoQuery.setPageNo(pageNo);
        videoInfoQuery.setOrderBy("create_time desc");
        videoInfoQuery.setRecommendType(VideoRecommendTypeEnum.NO_RECOMMEND.getType());
        PaginationResultVO videoList = videoInfoService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(videoList);
    }

    @RequestMapping("/getVideoInfo")
    public ResponseVO getVideoInfo(@NotEmpty String videoId) throws BusinessException {
        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if(videoInfo == null) {
            throw new BusinessException("videoInfo不存在");
        }
        TokenUserInfoDto userInfoDto = getTokenUserInfoDto();
        List<UserAction> userActionList = new ArrayList<>();
        if(userInfoDto != null) {
            UserActionQuery userActionQuery = new UserActionQuery();
            userActionQuery.setUserId(userInfoDto.getUserId());
            userActionQuery.setVideoId(videoId);
            userActionQuery.setActionTypeArray(new Integer[]{UserActionTypeEnum.VIDEO_LIKE.getType(), UserActionTypeEnum.VIDEO_COLLECT.getType(),
                    UserActionTypeEnum.VIDEO_COIN.getType()});

            userActionList = interactClient.getUserActionList(userActionQuery);
        }

        VideoInfoResultVO resultVO = new VideoInfoResultVO(videoInfo, userActionList);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/loadVideoPList")
    public ResponseVO loadVideoPList(@NotEmpty String videoId) {
        VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
        videoInfoFileQuery.setVideoId(videoId);
        videoInfoFileQuery.setOrderBy("file_index asc");
        List<VideoInfoFile> fileList = videoInfoFileService.findListByParam(videoInfoFileQuery);
        return getSuccessResponseVO(fileList);
    }

    @RequestMapping("/search")
    public ResponseVO search(@NotEmpty String keyword, @NotNull Integer orderType, Integer pageNo) throws BusinessException {
        //TODO 记录搜索热池
        redisComponent.addKeywordCount(keyword);
        PaginationResultVO paginationResultVO = esSearchComponent.search(true, keyword, orderType, pageNo, PageSize.SIZE30.getSize());
        return getSuccessResponseVO(paginationResultVO);
    }

    @RequestMapping("/getVideoRecommend")
    public ResponseVO getVideoRecommend(@NotEmpty String keyword, @NotEmpty String videoId) throws BusinessException {
        List<VideoInfo> videoInfoList = esSearchComponent.search(false, keyword, SearchOrderTypeEnum.VIDEO_PLAY.getType(), 1, PageSize.SIZE10.getSize()).getList();
        videoInfoList = videoInfoList.stream().filter(item->!item.getVideoId().equals(videoId)).toList();
        return getSuccessResponseVO(videoInfoList);
    }

    @RequestMapping("/getSearchKeywordTop")
    public ResponseVO getSearchKeywordTop() throws BusinessException {
        List<String> keywordTop = redisComponent.getKeywordTop(Constants.LENGTH_10);
        return getSuccessResponseVO(keywordTop);
    }
}
