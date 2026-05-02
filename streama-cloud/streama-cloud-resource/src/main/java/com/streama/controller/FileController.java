package com.streama.controller;

import com.streama.api.consumer.VideoClient;
import com.streama.annotation.GlobalInterceptor;
import com.streama.component.RedisComponent;
import com.streama.entity.config.AppConfig;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.SysSettingDto;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.dto.UploadFileDto;
import com.streama.entity.dto.VideoPlayInfoDto;
import com.streama.entity.po.VideoInfoFile;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.vo.ResponseVO;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.exception.BusinessException;
import com.streama.utils.DateUtils;
import com.streama.utils.FFmpegUtils;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

@RestController
@Validated
@Slf4j
public class FileController extends ABaseController {
    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private VideoClient videoClient;

    @Resource
    private FFmpegUtils ffmpegUtils;

    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotNull String sourceName) throws IOException, BusinessException {
        if (!StringTools.pathIsOk(sourceName)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String suffix = StringTools.getFileSuffix(sourceName);
        response.setContentType("image/" + suffix.replace(".", ""));
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, sourceName);
    }

    public void getVideoRawResource(HttpServletResponse response, @NotNull String sourceName) throws IOException, BusinessException {
        if (!StringTools.pathIsOk(sourceName)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        response.setContentType("video/mp4");
        response.setHeader("Cache-Control", "no-store");
        readFile(response, sourceName);
    }

    protected void readFile(HttpServletResponse response, String filePath) throws IOException {
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + filePath);
        if (!file.exists()) {
            return;
        }
        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(file)) {
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取文件异常", e);
        }
    }

    @RequestMapping("/preUploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO preUploadVideo(@NotEmpty String fileName, @NotNull Integer chunks) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        String uploadId = redisComponent.savePreVideoFileInfo(tokenUserInfoDto.getUserId(), fileName, chunks);
        return getSuccessResponseVO(uploadId);
    }

    @RequestMapping("/uploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadVideo(@NotNull MultipartFile chunkFile, @NotNull Integer chunkIndex, @NotEmpty String uploadId) throws BusinessException, IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UploadFileDto fileDto = redisComponent.getUploadVideoFileInfo(tokenUserInfoDto.getUserId(), uploadId);
        if (fileDto == null) {
            throw new BusinessException("文件不存在请重新上传");
        }
        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        if (fileDto.getFileSize() > sysSettingDto.getVideoSize() * Constants.MB_SIZE) {
            throw new BusinessException("文件超过大小限制");
        }

        if (chunkIndex - 1 > fileDto.getChunkIndex() || chunkIndex > fileDto.getChunks() - 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();
        File targetFile = new File(folder + "/" + chunkIndex);
        chunkFile.transferTo(targetFile);
        //设置当前索引
        fileDto.setChunkIndex(chunkIndex);
        //设置当前上传的总filesize
        fileDto.setFileSize(fileDto.getFileSize() + chunkFile.getSize());
        redisComponent.updateVideoFileInfo(tokenUserInfoDto.getUserId(), fileDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/deleteUploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO deleteUploadVideo(@NotEmpty String uploadId) throws BusinessException, IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UploadFileDto fileDto = redisComponent.getUploadVideoFileInfo(tokenUserInfoDto.getUserId(), uploadId);
        if (fileDto == null) {
            throw new BusinessException("文件不存在请重新上传");
        }
        redisComponent.deleteUploadVideoFileInfo(tokenUserInfoDto.getUserId(), uploadId);
        FileUtils.deleteDirectory(new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath()));
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws BusinessException, IOException {
        return getSuccessResponseVO(uploadImageInner(file, createThumbnail));
    }

    public String uploadImageInner(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws IOException, BusinessException {
        String day = DateUtils.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_COVER + day;
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        String fileName = file.getOriginalFilename();
        String fileSuffix = StringTools.getFileSuffix(fileName);
        String realFileName = StringTools.getRandomString(Constants.LENGTH_30) + fileSuffix;
        String filePath = folder + "/" + realFileName;
        file.transferTo(new File(filePath));
        if (createThumbnail != null && createThumbnail) {
            ffmpegUtils.createImageThumbnail(filePath);
        }
        return Constants.FILE_COVER + day + "/" + realFileName;
    }

    /**
     * 获取索引
     * @param fileId
     * @param response
     * @throws IOException
     */
    @RequestMapping("/videoResource/{fileId}")
    public void videoResource(@PathVariable("fileId") @NotEmpty String fileId, HttpServletResponse response) throws IOException {
        VideoFileStreamSource videoFileSource = resolveVideoFileStreamSource(fileId);
        if (videoFileSource == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        readFile(response, videoFileSource.getFilePath() + "/" + Constants.M3U8_NAME);
        if (!videoFileSource.isFormalFile()) {
            return;
        }

        VideoPlayInfoDto videoPlayInfoDto = new VideoPlayInfoDto();
        videoPlayInfoDto.setVideoId(videoFileSource.getVideoId());
        videoPlayInfoDto.setFileIndex(videoFileSource.getFileIndex());

        TokenUserInfoDto tokenUserInfoDto = getTokenInfoFromCookie();
        if(tokenUserInfoDto!=null) {
            videoPlayInfoDto.setUserId(tokenUserInfoDto.getUserId());
        }
        redisComponent.addVideoPlay(videoPlayInfoDto);
    }

    /**
     * 获取ts
     * @param response
     * @param fileId
     * @param ts
     * @throws IOException
     */
    @RequestMapping("/videoResource/{fileId}/{ts}")
    public void videoResourceTs(HttpServletResponse response, @PathVariable("fileId") @NotEmpty String fileId, @PathVariable("ts") @NotEmpty String ts) throws IOException {
        VideoFileStreamSource videoFileSource = resolveVideoFileStreamSource(fileId);
        if (videoFileSource == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        readFile(response, videoFileSource.getFilePath() + "/" + ts);
    }

    private VideoFileStreamSource resolveVideoFileStreamSource(String fileId) {
        VideoInfoFile videoInfoFile = videoClient.getVideoInfoFileByFileId(fileId);
        if (videoInfoFile != null) {
            return new VideoFileStreamSource(
                    videoInfoFile.getVideoId(),
                    videoInfoFile.getFileIndex(),
                    videoInfoFile.getFilePath(),
                    true
            );
        }

        VideoInfoFilePost videoInfoFilePost = videoClient.getVideoInfoFilePostByFileId(fileId);
        if (videoInfoFilePost == null) {
            return null;
        }

        return new VideoFileStreamSource(
                videoInfoFilePost.getVideoId(),
                videoInfoFilePost.getFileIndex(),
                videoInfoFilePost.getFilePath(),
                false
        );
    }

    private static class VideoFileStreamSource {
        private final String videoId;
        private final Integer fileIndex;
        private final String filePath;
        private final boolean formalFile;

        private VideoFileStreamSource(String videoId, Integer fileIndex, String filePath, boolean formalFile) {
            this.videoId = videoId;
            this.fileIndex = fileIndex;
            this.filePath = filePath;
            this.formalFile = formalFile;
        }

        public String getVideoId() {
            return videoId;
        }

        public Integer getFileIndex() {
            return fileIndex;
        }

        public String getFilePath() {
            return filePath;
        }

        public boolean isFormalFile() {
            return formalFile;
        }
    }

}
