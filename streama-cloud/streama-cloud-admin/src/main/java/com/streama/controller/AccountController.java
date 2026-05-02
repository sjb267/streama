package com.streama.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.streama.component.RedisComponent;
import com.streama.entity.config.AppConfig;
import com.streama.entity.constants.Constants;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
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
	private RedisComponent redisComponent;

	@Resource
	private DefaultKaptcha defaultKaptcha;

	@Resource
	private AppConfig appConfig;

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

	@RequestMapping("/login")
	public ResponseVO login(HttpServletRequest request,
							HttpServletResponse response,
							@NotEmpty String account,
							@NotEmpty String password,
							@NotEmpty String checkCodeKey,
							@NotEmpty String checkCode) throws BusinessException {
		try {
			if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("验证码错误");
			}

			if(!account.equals(appConfig.getAdminAccount()) || !password.equals(appConfig.getAdminPassword())) {
				throw new BusinessException("账号或密码错误");
			}
			String token = redisComponent.saveTokenInfo4Admin(account);
			saveToken2Cookie(response, token);
			return getSuccessResponseVO(account);
		} finally {
			redisComponent.cleanCheckCode(checkCodeKey);
			Cookie[] cookies = request.getCookies();
			String token = null;
			if(cookies != null) {
				//删除旧的cookie在redis中
				for (Cookie cookie : cookies) {
					if(cookie.getName().equals(Constants.TOKEN_ADMIN)) {
						token = cookie.getValue();
					}
				}
				if(!StringTools.isEmpty(token)){
					redisComponent.cleanToken4Admin(token);
				}
			}
		}
	}

	@RequestMapping("/logout")
	public ResponseVO logout(HttpServletResponse response) {
		cleanCookie(response);
		return getSuccessResponseVO(null);
	}
}
