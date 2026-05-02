package com.streama.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.dto.UserCountInfoDto;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.services.UserInfoService;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description:用户信息
Controller
 * @author:孙将斌
 * @date:2026/01/17
 */
@RestController
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController{

	@Resource
	private UserInfoService userInfoService;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private DefaultKaptcha defaultKaptcha;

	@RequestMapping("/checkCode")
	public ResponseVO checkCode() throws IOException {
		// 1. 使用Kaptcha生成验证码文本
		String code = defaultKaptcha.createText();

		//存验证码在redis
		String checkCodeKey = redisComponent.saveCheckCode(code);

		// 3. 根据文本生成图片，并转换为base64
		BufferedImage image = defaultKaptcha.createImage(code);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", outputStream);
		String checkCodeBase64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());

		checkCodeBase64 = "data:image/jpeg;base64," + checkCodeBase64;

		Map<String, String> result = new HashMap<>();
		//存唯一表示和验证码返回前端
		result.put("checkCode", checkCodeBase64);
		result.put("checkCodeKey", checkCodeKey);
		return getSuccessResponseVO(result);
	}

	@RequestMapping("/register")
	public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
							   @NotEmpty @Size(max = 20) String nickname,
							   @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
							   @NotEmpty String checkCodeKey,
							   @NotEmpty String checkCode) throws BusinessException {
		try {
			//校验验证码(从redis中获取验证码)
			if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("验证码错误");
			}
			userInfoService.register(email, nickname, registerPassword);
			return getSuccessResponseVO(null);
		} finally {
			redisComponent.cleanCheckCode(checkCodeKey);
		}
	}

	@RequestMapping("/login")
	public ResponseVO login(HttpServletRequest request,
							HttpServletResponse response,
							@NotEmpty @Email String email,
							@NotEmpty String password,
							@NotEmpty String checkCodeKey,
							@NotEmpty String checkCode) throws BusinessException {
		try {
			if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("验证码错误");
			}
			String ip = getIpAddr();
			//登录成功后返回token
			TokenUserInfoDto tokenUserInfoDto = userInfoService.login(email, password, ip);
			//将token存在Cookie中
			saveToken2Cookie(response, tokenUserInfoDto.getToken());
			return getSuccessResponseVO(tokenUserInfoDto);
		} finally {
			redisComponent.cleanCheckCode(checkCodeKey);
			Cookie[] cookies = request.getCookies();
			if(cookies != null) {
				String token = null;
				for (Cookie cookie : cookies) {
					if(cookie.getName().equals(Constants.TOKEN_WEB)) {
						token = cookie.getValue();
					}
				}
				if(!StringTools.isEmpty(token)){
					redisComponent.cleanToken(token);
				}
			}
		}
	}

	@RequestMapping("/autoLogin")
	public ResponseVO autoLogin(HttpServletResponse response) {
		//通过cookie里的token获取redis中的tokenUserInfoDto（可能cookie找不到token，或者token过期,导致获取空对象）
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
		//自动登录失败，直接返回
		if(tokenUserInfoDto == null) {
			return getSuccessResponseVO(null);
		}
		if(tokenUserInfoDto.getExpireAt() - System.currentTimeMillis() < Constants.REDIS_KEY_EXPIRES_ONE_DAY) {
			redisComponent.saveTokenInfo(tokenUserInfoDto);
			saveToken2Cookie(response, tokenUserInfoDto.getToken());
		}
		return getSuccessResponseVO(tokenUserInfoDto);
	}

	@RequestMapping("/logout")
	public ResponseVO logout(HttpServletResponse response) {
		cleanCookie(response);
		return getSuccessResponseVO(null);
	}

	@RequestMapping("/getUserCountInfo")
	public ResponseVO getUserCountInfo() throws BusinessException {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
		UserCountInfoDto userCountInfoDto = userInfoService.getUserCountInfo(tokenUserInfoDto.getUserId());
		return getSuccessResponseVO(userCountInfoDto);
	}
}
