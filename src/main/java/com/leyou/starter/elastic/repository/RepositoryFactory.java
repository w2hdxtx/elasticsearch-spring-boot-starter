package com.leyou.starter.elastic.repository;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 */
public class RepositoryFactory<T> implements FactoryBean<T> {

    private static final Logger log = LoggerFactory.getLogger(RepositoryFactory.class);

    private Class<T> interfaceType;
    private RestHighLevelClient client;

    public RepositoryFactory(Class<T> interfaceType, RestHighLevelClient client) {
        log.info("RepositoryFactory init ...");
        this.interfaceType = interfaceType;
        this.client = client;
    }

    @Override
    public T getObject() throws Exception {
        log.info("RepositoryBean proxy init ...");
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType},
                new ElasticSearchRepository<>(interfaceType, client));
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }
}
