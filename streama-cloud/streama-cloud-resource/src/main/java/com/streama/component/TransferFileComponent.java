package com.streama.component;

import com.streama.api.consumer.VideoClient;
import com.streama.entity.config.AppConfig;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.UploadFileDto;
import com.streama.entity.enums.VideoFileTransferResultEnum;
import com.streama.entity.enums.VideoStatusEnum;
import com.streama.entity.po.VideoInfoFilePost;
import com.streama.entity.po.VideoInfoPost;
import com.streama.entity.query.VideoInfoFilePostQuery;
import com.streama.exception.BusinessException;
import com.streama.utils.FFmpegUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

@Component
@Slf4j
public class TransferFileComponent {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FFmpegUtils ffmpegUtils;

    @Resource
    private VideoClient videoClient;

    public void transferVideoFile(VideoInfoFilePost videoInfoFilePost) {
        VideoInfoFilePost updateFilePost = new VideoInfoFilePost();
        try {
            //1.首先将上传的文件（在temp搬到正式目录video去)
            //从redis中获取上传文件的信息
            UploadFileDto fileDto = redisComponent.getUploadVideoFileInfo(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());

            String tempFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();
            File tempFile = new File(tempFilePath);

            String targetFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_VIDEO + fileDto.getFilePath();
            File targetFile = new File(targetFilePath);
            //拷贝
            FileUtils.copyDirectory(tempFile, targetFile);
            //删除临时目录
            FileUtils.forceDelete(tempFile);
            //删除redis中的上传文件信息
            redisComponent.deleteUploadVideoFileInfo(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());

            //2.合并文件
            String completeVideo = targetFilePath+Constants.TEMP_VIDEO_NAME;
            union(targetFilePath, completeVideo, true);

            //3.获取播放时长
            Integer duration = ffmpegUtils.getVideoInfoDuration(completeVideo);
            updateFilePost.setDuration(duration);
            updateFilePost.setFileSize(new File(completeVideo).length());
            updateFilePost.setFilePath(Constants.FILE_VIDEO+fileDto.getFilePath());
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());

            //4.将视频转换为ts格式
            convertVideo2Ts(completeVideo);
        } catch (Exception e) {
            log.error("文件转码失败", e);
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
        } finally {
            videoClient.transferVideoFile4Db(videoInfoFilePost.getVideoId(),
                    videoInfoFilePost.getUploadId(),
                    videoInfoFilePost.getUserId(),
                    updateFilePost);
        }
    }

    private void convertVideo2Ts(String completeVideo) throws BusinessException {
        File videoFile = new File(completeVideo);
        File tsFolder = videoFile.getParentFile();
        String codec = ffmpegUtils.getVideoCodec(completeVideo);
        if(Constants.VIDEO_CODE_HEVC.equals(codec)) {
            String tempFileName = completeVideo + Constants.VIDEO_CODE_TEMP_FILE_SUFFIX;
            new File(completeVideo).renameTo(new File(tsFolder, tempFileName));
            ffmpegUtils.convertHevc2Mp4(tempFileName, completeVideo);
            new File(tempFileName).delete();
        }

        ffmpegUtils.convertVideo2Ts(tsFolder,completeVideo);
        //TODO 不需要删除
//		videoFile.delete();
    }

    private void union(String dirPath, String toFilePath, Boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if(!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        try (RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")) {
            byte[] bytes = new byte[1024 * 10];
            for(int i=0; i<fileList.length; i++) {
                int len = -1;
                //创建读取文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(bytes)) != -1) {
                        writeFile.write(bytes, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (FileNotFoundException e) {
            throw new BusinessException("合并文件" + dirPath + "出错了");
        } catch (IOException e) {
            throw new BusinessException("合并文件失败1");
        } finally {
            if(delSource) {
                for(int i=0; i<fileList.length; i++) {
                    fileList[i].delete();
                }
            }
        }
    }
}
