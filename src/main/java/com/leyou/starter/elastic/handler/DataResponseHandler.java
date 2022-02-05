package com.leyou.starter.elastic.handler;

import org.elasticsearch.action.search.SearchResponse;

/**
 * 泛型说明：
 * R: 当前Response处理完成后的返回值类型
 * D: 索引库中存储的数据在Java中的对应的类型
 */
@FunctionalInterface
public interface DataResponseHandler<R, D> extends ResponseHandler<R> {

    default R handleResponse(SearchResponse response){
        return null;
    }

    R handleResponse(SearchResponse response, Class<D> dataType);
}
