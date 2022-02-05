package com.leyou.starter.elastic.handler;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregations;

public class AggregationResponseHandler implements ResponseHandler<Aggregations> {
    @Override
    public Aggregations handleResponse(SearchResponse response) {
        return response.getAggregations();
    }
}
