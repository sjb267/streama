package com.streama.api.provider;

import com.streama.annotation.GlobalInterceptor;
import com.streama.controller.FileController;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.dto.VideoPlayInfoDto;
import com.streama.entity.enums.DateTimePatternEnum;

import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.utils.DateUtils;
import com.streama.utils.StringTools;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/file")
public class ResourceApi {

    @Resource
    private FileController fileController;

    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public String uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws BusinessException, IOException {
        return fileController.uploadImageInner(file, createThumbnail);
    }

    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotNull String sourceName) throws IOException, BusinessException {
        fileController.getResource(response, sourceName);
    }

    @RequestMapping("/videoRaw")
    public void videoRaw(HttpServletResponse response, @NotNull String sourceName) throws IOException, BusinessException {
        fileController.getVideoRawResource(response, sourceName);
    }

    @RequestMapping("/videoResource/{fileId}")
    public void videoResource(@PathVariable("fileId") @NotEmpty String fileId, HttpServletResponse response) throws IOException {
        fileController.videoResource(fileId, response);
    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    public void videoResourceTs(HttpServletResponse response, @PathVariable("fileId") @NotEmpty String fileId, @PathVariable("ts") @NotEmpty String ts) throws IOException {
        fileController.videoResourceTs(response, fileId, ts);
    }
}
