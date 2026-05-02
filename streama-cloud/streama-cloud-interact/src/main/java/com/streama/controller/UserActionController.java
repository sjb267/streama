package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.UserAction;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.UserActionService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userAction")
@Validated
public class UserActionController extends ABaseController {

    @Resource
    private UserActionService userActionService;

    //用户行为
    @RequestMapping("/doAction")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO doAction(@NotEmpty String videoId, @NotNull Integer actionType,
                               Integer actionCount, Integer commentId) throws BusinessException {
        UserAction userAction = new UserAction();
        userAction.setUserId(getTokenUserInfoDto().getUserId());
        userAction.setActionType(actionType);
        userAction.setVideoId(videoId);
        actionCount = actionCount==null? Constants.ONE:actionCount;
        userAction.setActionCount(actionCount);
        commentId = commentId == null ? Constants.ZERO : commentId;
        userAction.setCommentId(commentId);
        userActionService.saveAction(userAction);
        return getSuccessResponseVO(null);
    }
}
