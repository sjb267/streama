package com.streama.api.consumer;

import com.streama.entity.constants.Constants;
import com.streama.entity.po.CategoryInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_ADMIN)
public interface CategoryClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/loadAllCategory")
    List<CategoryInfo> loadAllCategory();
}
