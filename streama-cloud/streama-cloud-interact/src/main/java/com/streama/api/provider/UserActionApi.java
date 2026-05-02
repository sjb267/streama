package com.streama.api.provider;

import com.streama.entity.constants.Constants;
import com.streama.entity.po.UserAction;
import com.streama.entity.query.UserActionQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.services.UserActionService;
import jakarta.annotation.Resource;
import org.apache.catalina.User;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/userAction")
public class UserActionApi {
    @Resource
    private UserActionService userActionService;

    @RequestMapping("/getUserActionList")
    public List<UserAction> getUserActionList(@RequestBody UserActionQuery userActionQuery) {
        return userActionService.findListByParam(userActionQuery);
    }
}
