package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.api.consumer.ResourceClient;
import com.streama.entity.vo.ResponseVO;
import feign.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/file")
@Validated
@Slf4j
public class FileController extends ABaseController {
    @Resource
    private ResourceClient resourceCLient;

    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail){
        return getSuccessResponseVO(resourceCLient.uploadImage(file, createThumbnail));
    }

    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse servletResponse, @NotNull String sourceName){
        Response response = resourceCLient.getResource(sourceName);
        convertFileResponse2Stream(servletResponse, response);
    }

    @RequestMapping("/videoResource/{fileId}")
    public void videoResource(@PathVariable("fileId") @NotEmpty String fileId, HttpServletResponse servletResponse){
        convertFileResponse2Stream(servletResponse, resourceCLient.videoResource(fileId));
    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    public void videoResourceTs(HttpServletResponse servletResponse, @PathVariable("fileId") @NotEmpty String fileId, @PathVariable("ts") @NotEmpty String ts){
        convertFileResponse2Stream(servletResponse, resourceCLient.getVideoResourceTs(fileId, ts));
    }

}
