package com.leyou.starter.elastic.handler;

import org.elasticsearch.action.search.SearchResponse;

public class NativeQueryResponseHandler implements ResponseHandler<SearchResponse> {
    @Override
    public SearchResponse handleResponse(SearchResponse response) {
        return response;
    }
}
