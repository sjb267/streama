package com.streama.controller;

import com.streama.entity.vo.ResponseVO;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


@RestControllerAdvice
public class AGlobalExceptionHandlerController {

    private static final String STATIC_ERROR = "error";
    private static final Logger logger = LoggerFactory.getLogger(AGlobalExceptionHandlerController.class);

    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request) {
        logger.error("请求错误，请求地址{}， 错误信息:", request.getRequestURL(), e);
        ResponseVO ajaxResponse = new ResponseVO();
        // 404
        if(e instanceof NoHandlerFoundException || e instanceof NoResourceFoundException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_404.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_404.getMsg());
            ajaxResponse.setStatus(STATIC_ERROR);
        } else if (e instanceof BusinessException) {
            // 业务错误
            BusinessException biz = (BusinessException) e;
            ajaxResponse.setCode(biz.getCode() == null ? ResponseCodeEnum.CODE_600.getCode() : biz.getCode());
            ajaxResponse.setInfo(biz.getMessage());
            ajaxResponse.setStatus(STATIC_ERROR);
        } else if(e instanceof ConstraintViolationException || e instanceof BindException || e instanceof MethodArgumentTypeMismatchException) {
            // 参数类型错误
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_600.getMsg());
            ajaxResponse.setStatus(STATIC_ERROR);
        } else if(e instanceof DuplicateKeyException) {
            // 主键冲突
            ajaxResponse.setCode(ResponseCodeEnum.CODE_601.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_601.getMsg());
            ajaxResponse.setStatus(STATIC_ERROR);
        } else {
            // 服务器返回错误
            ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_500.getMsg());
            ajaxResponse.setStatus(STATIC_ERROR);
        }
        return ajaxResponse;
    }
}
