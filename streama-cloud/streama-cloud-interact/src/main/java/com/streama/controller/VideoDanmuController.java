package com.streama.controller;

import com.streama.annotation.GlobalInterceptor;
import com.streama.api.consumer.VideoClient;
import com.streama.entity.constants.Constants;
import com.streama.entity.po.VideoDanmu;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.VideoDanmuQuery;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.VideoDanmuService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;

@RestController("videoDanmuController")
@RequestMapping("/danmu")
@Slf4j
public class VideoDanmuController extends ABaseController{

	@Resource
	private VideoDanmuService videoDanmuService;

    @Resource
    private VideoClient videoClient;

	/**
	 * 根据DanmuId删除
	 */
	@RequestMapping("postDanmu")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO postDanmu(@NotEmpty String videoId, @NotEmpty String fileId,
								@NotEmpty @Size(max = 200) String text, @NotNull Integer mode,
								@NotEmpty String color, @NotNull Integer time) throws BusinessException {
		VideoDanmu videoDanmu = new VideoDanmu();
		videoDanmu.setVideoId(videoId);
		videoDanmu.setFileId(fileId);
		videoDanmu.setText(text);
		videoDanmu.setMode(mode);
		videoDanmu.setColor(color);
		videoDanmu.setTime(time);
		videoDanmu.setUserId(getTokenUserInfoDto().getUserId());
		videoDanmu.setPostTime(new Date());

		videoDanmuService.saveVideoDanmu(videoDanmu);
		return getSuccessResponseVO(null);
	}

	@RequestMapping("loadDanmu")
	public ResponseVO loadDanmu(@NotEmpty String fileId, @NotEmpty String videoId) {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
		if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ONE.toString())) {
			return getSuccessResponseVO(new ArrayList<>());
		}
		VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
		videoDanmuQuery.setFileId(fileId);
		videoDanmuQuery.setOrderBy("danmu_id asc");
		return getSuccessResponseVO(videoDanmuService.findListByParam(videoDanmuQuery));
	}
}
