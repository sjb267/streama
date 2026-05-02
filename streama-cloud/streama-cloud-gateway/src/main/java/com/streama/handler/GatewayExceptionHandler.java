package com.streama.handler;

import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.vo.ResponseVO;
import com.streama.exception.BusinessException;
import com.streama.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    protected static final String STATIC_ERROR = "error";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ResponseVO responseVO = getResponse(ex);
        ServerHttpResponse response = exchange.getResponse();

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtils.convertObj2Json(responseVO).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }

    private ResponseVO getResponse(Throwable ex) {
        ResponseVO responseVO = new ResponseVO();
        if(ex instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) ex;
            if(HttpStatus.NOT_FOUND == statusException.getStatusCode()) {
                responseVO.setCode(ResponseCodeEnum.CODE_404.getCode());
                responseVO.setInfo(ResponseCodeEnum.CODE_404.getMsg());
                responseVO.setStatus(STATIC_ERROR);
                return responseVO;
            } else if (HttpStatus.SERVICE_UNAVAILABLE == statusException.getStatusCode()) {
                responseVO.setCode(ResponseCodeEnum.CODE_503.getCode());
                responseVO.setInfo(ResponseCodeEnum.CODE_503.getMsg());
                return responseVO;
            } else {
                responseVO.setCode(statusException.getStatusCode().value());
                responseVO.setInfo(statusException.getMessage());
                return responseVO;
            }
        } else if(ex instanceof BusinessException) {
            BusinessException businessException = (BusinessException) ex;
            responseVO.setCode(businessException.getCode());
            responseVO.setInfo(businessException.getMessage());
            return responseVO;
        }
        responseVO.setCode(ResponseCodeEnum.CODE_500.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        return responseVO;
    }
}
