package com.streama.utils;

import com.streama.entity.config.AppConfig;
import com.streama.entity.constants.Constants;
import com.streama.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;

@Component
public class FFmpegUtils {
    @Resource
    private AppConfig appConfig;

    public void createImageThumbnail(String filePath) throws BusinessException {
        String CMD = "ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\"";
        CMD = String.format(CMD, filePath, filePath + Constants.IMAGE_THUMBNAIL_SUFFIX);
        ProcessUtils.executeCommand(CMD, false);
    }

    /**
     * 获取视频时长
     */
    public Integer getVideoInfoDuration(String completeVideo) throws BusinessException {
        final String CMD_GET_CODE = "ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"%s\"";
        String cmd = String.format(CMD_GET_CODE, completeVideo);
        String result = ProcessUtils.executeCommand(cmd, appConfig.isShowFFmpegLog());
        if(StringTools.isEmpty(result)) {
            return 0;
        }
        result = result.replace("\n", "");
        return new BigDecimal(result).intValue();
    }

    public String getVideoCodec(String videoFilePath) throws BusinessException {
        final String CMD_GET_CODE = "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name \"%s\"";
        String cmd = String.format(CMD_GET_CODE, videoFilePath);
        String result = ProcessUtils.executeCommand(cmd, appConfig.isShowFFmpegLog());
        result = result.replace("\n", "");
        result = result.substring(result.indexOf("=") + 1);
        String codec = result.substring(0, result.indexOf("["));
        return codec;
    }

    public void convertHevc2Mp4(String newFileName, String videoFilePath) throws BusinessException {
        String CMD_HEVC_264  = "ffmpeg -i \"%s\" -c:v libx264 -crf 20 \"%s\" -y";
        String cmd = String.format(CMD_HEVC_264, videoFilePath, newFileName);
        ProcessUtils.executeCommand(cmd, appConfig.isShowFFmpegLog());
    }

    public void convertVideo2Ts(File tsFolder, String videoFilePath) throws BusinessException {
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i \"%s\" -vcodec copy -acodec copy -bsf:v h264_mp4toannexb \"%s\"";
        //10秒分一次
        final String CMD_CUT_TS = "ffmpeg -i \"%s\" -c copy -map 0 -f segment -segment_list \"%s\" -segment_time 10 %s/%%4d.ts";
        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts文件
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, appConfig.isShowFFmpegLog());
        //生成索引文件.m3u8和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath());
        ProcessUtils.executeCommand(cmd, appConfig.isShowFFmpegLog());
        //删除index.ts
        new File(tsPath).delete();
    }
}
