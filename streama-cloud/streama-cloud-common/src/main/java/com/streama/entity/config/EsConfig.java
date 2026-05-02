package com.streama.entity.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig implements DisposableBean {

    @Resource
    private AppConfig appConfig;

    private RestClient restClient;
    private ElasticsearchTransport transport;

    @Bean
    public RestClient restClient() {
        // 解析 esHostPort，格式如 "localhost:9200"
        String[] hostPort = appConfig.getEsHostPort().split(":");
        restClient = RestClient.builder(
                new HttpHost(hostPort[0], Integer.parseInt(hostPort[1]), "http")
        ).build();
        return restClient;
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return transport;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    @Override
    public void destroy() throws Exception {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }
}