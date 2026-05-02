package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.api.consumer.VideoClient;
import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.enums.CommentTopTypeEnum;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.entity.po.UserAction;
import com.streama.entity.po.VideoComment;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.UserActionQuery;
import com.streama.entity.query.VideoCommentQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.entity.vo.VideoCommentActionResultVO;
import com.streama.exception.BusinessException;
import com.streama.services.UserActionService;
import com.streama.services.VideoCommentService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/comment")
@Validated
public class VideoCommentController extends ABaseController {

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private UserActionService userActionService;

    @Resource
    private VideoClient videoClient;

    @RequestMapping("/postComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO postComment(@NotEmpty String videoId,
                                  @NotEmpty @Size(max=500) String content,
                                  Integer replyCommentId,
                                  @Size(max = 50) String imgPath) throws BusinessException {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoComment comment = new VideoComment();
        comment.setUserId(tokenUserInfoDto.getUserId());
        comment.setAvatar(tokenUserInfoDto.getAvatar());
        comment.setNickName(tokenUserInfoDto.getNickName());
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setImgPath(imgPath);
        videoCommentService.postComment(comment, replyCommentId);
        return getSuccessResponseVO(comment);
    }

    //用户行为
    @RequestMapping("loadComment")
    public ResponseVO loadComment(@NotEmpty String videoId,
                                  Integer pageNo,
                                  Integer orderType) {
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
        //应对博主突然关闭了评论区
        if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())) {
            return getSuccessResponseVO(new ArrayList<>());
        }
        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setVideoId(videoId);
        commentQuery.setLoadChildren(true);
        commentQuery.setPageNo(pageNo);
        commentQuery.setPageSize(PageSize.SIZE15.getSize());
        commentQuery.setPCommentId(0);
        //根据orderType排序（0：按点赞数排序，1：按评论时间排序）
        String orderBy = orderType == null || orderType == 0 ? "like_count desc, comment_id desc" : "comment_id desc";
        commentQuery.setOrderBy(orderBy);

        PaginationResultVO<VideoComment> commentData = videoCommentService.findListByPage(commentQuery);

        //滚动形式，获取全部评论的情况下将置顶评论放到最前面
        if (pageNo == null) {
            List<VideoComment> topCommentList = topComment(videoId);
            if (!topCommentList.isEmpty()) {
                Integer topId = topCommentList.get(0).getCommentId();

                List<VideoComment> commentList = new ArrayList<>(
                        commentData.getList().stream()
                                .filter(item -> !Objects.equals(item.getCommentId(), topId)) // 注意这里应是排除置顶，避免重复
                                .toList()
                );

                commentList.addAll(0, topCommentList);
                commentData.setList(commentList);
            }
        }

        VideoCommentActionResultVO resultVO = new VideoCommentActionResultVO();
        resultVO.setCommentData(commentData);

        List<UserAction> userActionList = new ArrayList<>();
        //查看是否登录（有则获取用户状态）
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        if(tokenUserInfoDto != null) {
            UserActionQuery userActionQuery = new UserActionQuery();
            userActionQuery.setUserId(tokenUserInfoDto.getUserId());
            userActionQuery.setVideoId(videoId);
            userActionQuery.setActionTypeArray(new Integer[]{UserActionTypeEnum.COMMENT_LIKE.getType(), UserActionTypeEnum.COMMENT_HATE.getType()});
            userActionList = userActionService.findListByParam(userActionQuery);
        }

        resultVO.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVO);
    }

    private List<VideoComment> topComment(String videoId) {
        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
        videoCommentQuery.setLoadChildren(true);
        List<VideoComment> videoCommentList = videoCommentService.findListByParam(videoCommentQuery);
        return videoCommentList;
    }

    @RequestMapping("/topComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO topComment(@NotNull Integer commentId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.topComment(commentId, tokenUserInfoDto.getUserId());

        return getSuccessResponseVO(null);
    }

    @RequestMapping("/cancelTopComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelTopComment(@NotNull Integer commentId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.cancelTopComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/userDelComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO userDelComment(@NotNull Integer commentId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
