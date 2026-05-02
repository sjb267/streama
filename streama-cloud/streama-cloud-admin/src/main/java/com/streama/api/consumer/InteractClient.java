package com.streama.api.consumer;

import com.streama.entity.constants.Constants;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = Constants.SERVER_NAME_INTERACT)
public interface InteractClient {
}
