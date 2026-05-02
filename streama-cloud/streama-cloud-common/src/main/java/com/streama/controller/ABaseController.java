package com.streama.controller;

import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.vo.ResponseVO;
import com.streama.entity.enums.ResponseCodeEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

;

public class ABaseController {
    protected static final String STATIC_SUCCESS = "success";
    protected static final String STATIC_ERROR = "error";

    @Resource
    private RedisComponent redisComponent;

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<T>();
        responseVO.setStatus(STATIC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected String getIpAddr() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            if(ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    protected TokenUserInfoDto getTokenUserInfoDto() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(Constants.TOKEN_WEB);
        return redisComponent.getTokenInfo(token);
    }

    protected void saveToken2Cookie(HttpServletResponse response, String token) {
        //会覆盖旧的cookie值
        Cookie cookie = new Cookie(Constants.TOKEN_WEB, token);
        cookie.setMaxAge(Constants.TIME_SECONDS_DAY);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    protected void cleanCookie(HttpServletResponse response) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Cookie[] cookies = request.getCookies();
        if(cookies == null) {
            return;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(Constants.TOKEN_WEB)) {
                redisComponent.cleanToken4Admin(cookie.getValue());
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
                break;
            }
        }
    }

    public TokenUserInfoDto getTokenInfoFromCookie() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = getTokenFromCookie(request);
        if(token == null) {
            return null;
        }
        return redisComponent.getTokenInfo(token);
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if(cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equalsIgnoreCase(Constants.TOKEN_WEB)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
