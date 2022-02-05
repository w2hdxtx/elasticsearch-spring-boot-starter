package com.leyou.starter.elastic.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@Configuration
@ConditionalOnClass({Mono.class, Flux.class, RestHighLevelClient.class})
@ConditionalOnProperty(value = "elasticsearch.hosts")
public class ElasticSearchAutoConfiguration implements ApplicationContextAware {

    private String hosts;

    private RestHighLevelClient restHighLevelClient() {
        return new RestHighLevelClient(
                RestClient.builder(
                        Stream.of(StringUtils.split(hosts, ","))
                                .map(HttpHost::create).toArray(HttpHost[]::new)
                )
        );
    }

    @Bean
    public RepositoryScanner repositoryScanner() {
        return new RepositoryScanner(restHighLevelClient());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.hosts = applicationContext.getEnvironment().getProperty("elasticsearch.hosts");
    }
}
