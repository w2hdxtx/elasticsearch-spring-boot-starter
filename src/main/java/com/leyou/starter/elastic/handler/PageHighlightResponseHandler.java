package com.leyou.starter.elastic.handler;

import com.leyou.starter.elastic.entity.PageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageHighlightResponseHandler<T> implements DataResponseHandler<PageInfo<T>, T> {

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
            // 反序列化
            T t = null;
            try {
                t = mapper.readValue(hit.getSourceAsString(), dataType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            list.add(t);
            // 高亮处理
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            for (HighlightField highlightField : highlightFields.values()) {
                String fieldName = highlightField.getName();
                String value = StringUtils.join(highlightField.getFragments());
                try {
                    BeanUtils.setProperty(t, fieldName, value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new PageInfo<>(total, list);
    }
}
