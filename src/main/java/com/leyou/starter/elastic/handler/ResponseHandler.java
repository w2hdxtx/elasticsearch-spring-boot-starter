package com.leyou.starter.elastic.handler;

import org.elasticsearch.action.search.SearchResponse;

/**
 * 对SuggestResponse做处理
 * 泛型说明：
 * R: 当前Response处理完成后的返回值类型
 */
@FunctionalInterface
public interface ResponseHandler<R> {

    R handleResponse(SearchResponse response);
}
