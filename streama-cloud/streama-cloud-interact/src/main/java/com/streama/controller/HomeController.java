package com.streama.controller;

import com.streama.entity.enums.UserActionTypeEnum;
import com.streama.entity.query.UserActionQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.UserActionService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@Validated
public class HomeController extends ABaseController{

    @Resource
    private UserActionService userActionService;


    @RequestMapping("/loadUserCollection")
    public ResponseVO loadUserCollection(@NotEmpty String userId, Integer pageNo) throws BusinessException {
        UserActionQuery actionQuery = new UserActionQuery();
        actionQuery.setUserId(userId);
        actionQuery.setPageNo(pageNo);
        actionQuery.setOrderBy("action_time desc");
        actionQuery.setActionType(UserActionTypeEnum.VIDEO_COLLECT.getType());
        actionQuery.setQueryVideoInfo(true);
        PaginationResultVO resultVO = userActionService.findListByPage(actionQuery);
        return getSuccessResponseVO(resultVO);
    }
}
