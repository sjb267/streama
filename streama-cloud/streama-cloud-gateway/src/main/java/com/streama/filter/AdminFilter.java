package com.streama.filter;

import com.streama.entity.constants.Constants;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.exception.BusinessException;
import com.streama.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminFilter extends AbstractGatewayFilterFactory {

    private final static String URL_ACCOUNT = "/account";

    private final static String URL_FILE = "/file";

    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String rawPath = request.getURI().getRawPath();

            if(rawPath.contains(URL_ACCOUNT)){
                return chain.filter(exchange);
            }

            String token = getToken(request);
            if(rawPath.contains(URL_FILE)){
                token = getTokenFromCookie(request);
            }

            if(StringTools.isEmpty(token)) {
                try {
                    throw new BusinessException(ResponseCodeEnum.CODE_901);
                } catch (BusinessException e) {
                    throw new RuntimeException(e);
                }
            }

            return chain.filter(exchange);
        });
    }

    private String getToken(ServerHttpRequest request) {
        return request.getHeaders().getFirst(Constants.TOKEN_ADMIN);
    }

    private String getTokenFromCookie(ServerHttpRequest request) {
        return request.getCookies().getFirst(Constants.TOKEN_ADMIN).getValue();
    }
}
