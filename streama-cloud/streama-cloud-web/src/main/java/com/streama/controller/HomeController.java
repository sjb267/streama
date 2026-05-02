package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.po.UserInfo;
import com.streama.entity.query.UserFocusQuery;
import com.streama.entity.query.VideoInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.entity.vo.UserInfoVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.VideoOrderTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.services.UserFocusService;
import com.streama.services.UserInfoService;
import com.streama.services.VideoInfoService;
import com.streama.utils.CopyTools;
import com.streama.utils.DateUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@Validated
public class HomeController extends ABaseController{

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserFocusService userFocusService;

    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@NotEmpty String userId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserInfo userInfo = userInfoService.getUserDetailInfo(tokenUserInfoDto == null ? null : tokenUserInfoDto.getUserId(), userId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/updateUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updateUserInfo(@NotEmpty @Size(max = 20) String nickName,
                                     @NotEmpty @Size(max = 100) String avatar,
                                     @NotNull Integer sex,
                                     @NotEmpty @Size(max = 10) String birthday,
                                     @NotEmpty @Size(max = 150) String school,
                                     @NotEmpty @Size(max = 80) String personIntroduction,
                                     @Size(max = 300) String noticeInfo) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserInfo userInfo = new UserInfo();
        userInfo.setNickName(nickName);
        userInfo.setAvatar(avatar);
        userInfo.setSex(sex);
        userInfo.setBirthday(DateUtils.parse(birthday, "yyyy-MM-dd"));
        userInfo.setSchool(school);
        userInfo.setPersonIntroduction(personIntroduction);
        userInfo.setNoticeInfo(noticeInfo);
        userInfoService.updateUserInfo(userInfo, tokenUserInfoDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/focus")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO focus(@NotEmpty String focusUserId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userFocusService.focusUser(tokenUserInfoDto.getUserId(), focusUserId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/cancelFocus")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelFocus(@NotEmpty String focusUserId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userFocusService.cancelFocusUser(tokenUserInfoDto.getUserId(), focusUserId);
        return getSuccessResponseVO(null);
    }

    //只有自己的关注列表才能看
    @RequestMapping("/loadFocusList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadFocusList(Integer pageNo) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserFocusQuery userFocusQuery = new UserFocusQuery();
        userFocusQuery.setUserId(tokenUserInfoDto.getUserId());
        userFocusQuery.setPageNo(pageNo);
        userFocusQuery.setOrderBy("focus_time desc");
        userFocusQuery.setQueryType(Constants.ZERO);
        PaginationResultVO resultVO = userFocusService.findListByPage(userFocusQuery);
        return getSuccessResponseVO(resultVO);
    }

    //只有自己的关注列表才能看
    @RequestMapping("/loadFansList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadFansList(Integer pageNo) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserFocusQuery userFocusQuery = new UserFocusQuery();
        userFocusQuery.setFocusUserId(tokenUserInfoDto.getUserId());
        userFocusQuery.setPageNo(pageNo);
        userFocusQuery.setOrderBy("focus_time desc");
        userFocusQuery.setQueryType(Constants.ONE);
        PaginationResultVO resultVO = userFocusService.findListByPage(userFocusQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/loadVideoList")
    public ResponseVO loadVideoList(@NotEmpty String userId, Integer type, Integer pageNo, String videoName, Integer orderType) throws BusinessException {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        if(type != null) {
            videoInfoQuery.setPageSize(PageSize.SIZE10.getSize());
        }
        VideoOrderTypeEnum videoOrderTypeEnum = VideoOrderTypeEnum.getByType(orderType);
        if(videoOrderTypeEnum == null) {
            videoOrderTypeEnum = VideoOrderTypeEnum.CREATE_TIME;
        }
        videoInfoQuery.setOrderBy(videoOrderTypeEnum.getField() + " desc");
        videoInfoQuery.setVideoNameFuzzy(videoName);
        videoInfoQuery.setPageNo(pageNo);
        videoInfoQuery.setUserId(userId);
        PaginationResultVO resultVO = videoInfoService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

}
