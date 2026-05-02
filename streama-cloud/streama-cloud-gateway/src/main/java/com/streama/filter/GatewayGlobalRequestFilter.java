package com.streama.filter;

import com.streama.entity.constants.Constants;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GatewayGlobalRequestFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawpath = exchange.getRequest().getURI().getRawPath();
        log.info("请求的路劲{}", rawpath);
        if(rawpath.contains(Constants.INNER_API_PREFIX)) {
            return Mono.error(new BusinessException(ResponseCodeEnum.CODE_404));
        }
        return chain.filter(exchange);
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
