package com.leyou.starter.elastic.handler;

import com.leyou.starter.elastic.entity.PageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PageResponseHandler<T> implements DataResponseHandler<PageInfo<T>, T> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public PageInfo<T> handleResponse(SearchResponse response, Class<T> dataType) {
        SearchHits searchHits = response.getHits();
        // 总条数
        long total = searchHits.getTotalHits().value;
        // 数据
        SearchHit[] hits = searchHits.getHits();
        // 处理数据
        List<T> list = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            T t = null;
            try {
                t = mapper.readValue(hit.getSourceAsString(), dataType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 反序列化
            list.add(t);
        }
        return new PageInfo<>(total, list);
    }
}
