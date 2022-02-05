package com.leyou.starter.elastic.handler;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SuggestResponseHandler implements ResponseHandler<List<String>> {

    @Override
    public List<String> handleResponse(SearchResponse response) {
        return StreamSupport.stream(response.getSuggest().spliterator(), true)
                .map(s -> (CompletionSuggestion)s)
                .map(CompletionSuggestion::getOptions)
                .flatMap(List::stream)
                .map(CompletionSuggestion.Entry.Option::getText)
                .map(Text::string)
                .distinct()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
