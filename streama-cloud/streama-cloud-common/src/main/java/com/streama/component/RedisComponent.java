package com.streama.component;

import com.streama.entity.config.AppConfig;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.SysSettingDto;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.dto.UploadFileDto;
import com.streama.entity.dto.VideoPlayInfoDto;
import com.streama.entity.po.CategoryInfo;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.enums.DateTimePatternEnum;
import com.streama.redis.RedisUtils;
import com.streama.utils.DateUtils;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private AppConfig appConfig;

    /**
     * 保存验证码到Redis
     * @param code 验证码文本
     * @return 验证码唯一表示
     */
    public String saveCheckCode(String code) {
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, code, Constants.REDIS_KEY_EXPIRES_ONE_MIN);
        return checkCodeKey;
    }

     /**
      * 获取Redis中的验证码
      * @param checkCodeKey 验证码唯一表示
      * @return 验证码文本
      */
    public String getCheckCode(String checkCodeKey) {
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

     /**
      * 清理Redis中的验证码
      * @param checkCodeKey 验证码唯一表示
      */
    public void cleanCheckCode(String checkCodeKey) {
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public void saveTokenInfo(TokenUserInfoDto tokenUserInfoDto) {
        String token = UUID.randomUUID().toString();
        tokenUserInfoDto.setExpireAt(System.currentTimeMillis() + Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7L);
        tokenUserInfoDto.setToken(token);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7L);
    }

    public void updateTokenInfo(TokenUserInfoDto tokenUserInfoDto) {
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + tokenUserInfoDto.getToken(), tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7L);
    }

    public String saveTokenInfo4Admin(String account) {
        String token = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN + token, account, Constants.REDIS_KEY_EXPIRES_ONE_DAY);
        return token;
    }

    public void cleanToken(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public void cleanToken4Admin(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public TokenUserInfoDto getTokenInfo(String token) {
        return (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public String getTokenInfo4Admin(String token) {
        return (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public void saveCategoryInfoList(List<CategoryInfo> categoryInfoList) {
        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST, categoryInfoList);
    }

    public List<CategoryInfo> getCategoryInfoList() {
        return (List<CategoryInfo>) redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
    }

    public String savePreVideoFileInfo(String userId, String fileName, Integer chunks) {
        String uploadId = StringTools.getRandomString(Constants.LENGTH_15);
        UploadFileDto uploadFileDto = new UploadFileDto();
        uploadFileDto.setChunks(chunks);
        uploadFileDto.setFileName(fileName);
        uploadFileDto.setUploadId(uploadId);
        uploadFileDto.setChunkIndex(0);
        String day = DateUtils.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String filePath = day + "/" + userId + uploadId;
        //创建每个用户预上传文件目录
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + filePath;
        File folderFile = new File(folder);
        if(!folderFile.exists()) {
            folderFile.mkdirs();
        }
        uploadFileDto.setFilePath(filePath);
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId, uploadFileDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY);
        return uploadId;
    }

    public UploadFileDto getUploadVideoFileInfo(String userId, String uploadId) {
        return (UploadFileDto) redisUtils.get(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId);
    }

    public SysSettingDto getSysSettingDto() {
        SysSettingDto sysSettingDto = (SysSettingDto)redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        //若没有则直接使用默认的
        if(sysSettingDto == null) {
            sysSettingDto = new SysSettingDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
        }
        return sysSettingDto;
    }

    public void updateVideoFileInfo(String userId, UploadFileDto fileDto) {
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE + userId + fileDto.getUploadId(), fileDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }

    public void deleteUploadVideoFileInfo(String userId, String uploadId) {
        redisUtils.delete(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId);
    }

    public void addFile2DelQueue(String videoId, List<String> filePathList) {
        redisUtils.lpushAll(Constants.REDIS_KEY_FILE_DEL + videoId, filePathList, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }

    public List<String> getDelFileList(String videoId) {
        return (List<String>) redisUtils.getQueueList(Constants.REDIS_KEY_FILE_DEL + videoId);
    }

    public void cleanDelFileList(String videoId) {
        redisUtils.delete(Constants.REDIS_KEY_FILE_DEL + videoId);
    }

    public void addFile2TransferQueue(List<VideoInfoFilePost> addFileList) {
        redisUtils.lpushAll(Constants.REDIS_KEY_QUEUE_TRANSFER , addFileList, 0);
    }

    public VideoInfoFilePost getFileFromTransferQueue() {
        return (VideoInfoFilePost) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_TRANSFER);
    }

    public void addKeywordCount(String keyword){
        redisUtils.zaddCount(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, keyword);
    }

    public List<String> getKeywordTop(Integer top){
        return redisUtils.getZSetList(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, top-1);
    }

    public void addVideoPlay(VideoPlayInfoDto videoPlayInfoDto) {
        redisUtils.lpush(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY, videoPlayInfoDto, null);
    }

    public VideoPlayInfoDto getVideoPlayFromVideoPlayQueue() {
        return (VideoPlayInfoDto) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY);
    }
}
