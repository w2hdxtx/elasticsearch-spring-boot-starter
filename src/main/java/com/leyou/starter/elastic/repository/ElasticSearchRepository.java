package com.leyou.starter.elastic.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.starter.elastic.annotation.IndexID;
import com.leyou.starter.elastic.annotation.Indices;
import com.leyou.starter.elastic.entity.PageInfo;
import com.leyou.starter.elastic.handler.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateResponse;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;

/**
 */
@SuppressWarnings("all")
public class ElasticSearchRepository<T, ID> implements Repository<T, ID>, InvocationHandler {

    private RestHighLevelClient client;

    /**
     * 实体类类型
     */
    private Class<T> clazz;
    private String indexName;
    /**
     * 实体类中id字段的名称
     */
    private Class<ID> idType;
    private String id;
    private Field idField;

    private static final ObjectMapper mapper = new ObjectMapper();
    public ElasticSearchRepository(Class<T> interfaceType, RestHighLevelClient client) {
        this.client = client;
        // 获取当前类上的泛型类型
        ParameterizedType parameterizedType = (ParameterizedType) interfaceType.getGenericInterfaces()[0];
        // 获取泛型对应的真实类型,这里有2个，T和ID
        Type[] actualType = parameterizedType.getActualTypeArguments();
        // 我们取数组的第一个，肯定是T的类型，即实体类类型
        this.clazz  = (Class<T>) actualType[0];
        this.idType = (Class<ID>) actualType[1];



        // 利用反射获取注解
        if (clazz.isAnnotationPresent(Indices.class)) {
            Indices indices = clazz.getAnnotation(Indices.class);
            // 获取索引库及类型名称
            indexName = indices.value();
        }else{
            // 没有注解，我们用类名称首字母小写，作为索引库名称
            String simpleName = clazz.getSimpleName();
            indexName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
        }


        // 获取带有@IndexID注解的字段：
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(IndexID.class)) {
                id = field.getName();
                idField = field;
            }
        }
        if (StringUtils.isBlank(id)) {
            // 没有找到id字段，则抛出异常
            throw new RuntimeException("实体类中必须有一个字段标记@IndexID注解。");
        }
    }

    public Boolean createIndex(String source) {
        try {
            return client.indices().create(
                    new CreateIndexRequest(indexName).source(source, XContentType.JSON),
                    RequestOptions.DEFAULT).isAcknowledged();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean deleteIndex() {
        try {
            return client.indices().delete(new DeleteIndexRequest(indexName),
                    RequestOptions.DEFAULT).isAcknowledged();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean save(T t) {
        try {
            return client.index(new IndexRequest(indexName).id(getID(t)).source(toJson(t), XContentType.JSON),
                    RequestOptions.DEFAULT).status() == RestStatus.CREATED;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean saveAll(Iterable<T> iterable) {
        BulkRequest request = new BulkRequest();
        iterable.forEach(t -> request.add(new IndexRequest(indexName).id(getID(t)).source(toJson(t), XContentType.JSON)));
        try {
            BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
            if(bulkResponse.status() != RestStatus.OK){
                return false;
            }
            if(bulkResponse.hasFailures()){
                throw new RuntimeException(bulkResponse.buildFailureMessage());
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean  deleteById(ID id) {
        try {
            return client.delete(new DeleteRequest(indexName, id.toString()),
                    RequestOptions.DEFAULT).status() == RestStatus.OK;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<T> queryById(ID id) {
        return Mono.create(sink -> {
            client.getAsync(new GetRequest(indexName, id.toString()),
                    RequestOptions.DEFAULT, new ActionListener<GetResponse>() {
                        @Override
                        public void onResponse(GetResponse response) {
                            sink.success(fromJson(response.getSourceAsString()));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            sink.error(e);
                        }
                    });
        });
    }

    public <R> Mono<R> queryBySourceBuilder(SearchSourceBuilder sourceBuilder, DataResponseHandler<R, T> handler) {
        return Mono.create(sink -> {
            SearchRequest request = new SearchRequest(indexName);
            request.source(sourceBuilder);
            client.searchAsync(request, RequestOptions.DEFAULT, new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse response) {
                    sink.success(handler.handleResponse(response, clazz));
                }

                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    public Mono<PageInfo<T>> queryBySourceBuilderForPage(SearchSourceBuilder sourceBuilder) {
        return queryBySourceBuilder(sourceBuilder, new PageResponseHandler<T>());
    }

    public Mono<PageInfo<T>> queryBySourceBuilderForPageHighlight(SearchSourceBuilder sourceBuilder) {
        return queryBySourceBuilder(sourceBuilder, new PageHighlightResponseHandler<T>());
    }

    public <R> Mono<R> queryByTemplate(String templateId, Map<String, Object> params, DataResponseHandler<R, T> handler) {
        return Mono.create(sink -> {
            SearchTemplateRequest request = new SearchTemplateRequest(new SearchRequest(indexName));
            request.setScriptType(ScriptType.STORED);
            request.setScript(templateId);
            request.setScriptParams(params);
            client.searchTemplateAsync(request, RequestOptions.DEFAULT, new ActionListener<SearchTemplateResponse>() {
                @Override
                public void onResponse(SearchTemplateResponse response) {
                    sink.success(handler.handleResponse(response.getResponse(), clazz));
                }

                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    public Mono<PageInfo<T>> queryByTemplateForPage(String templateId, Map<String, Object> params) {
        return queryByTemplate(templateId, params, new PageResponseHandler<T>());
    }

    public Mono<PageInfo<T>> queryByTemplateForPageHighlight(String templateId, Map<String, Object> params) {
        return queryByTemplate(templateId, params, new PageHighlightResponseHandler<T>());
    }

    public Mono<Aggregations> aggregationBySourceBuilder(SearchSourceBuilder sourceBuilder) {
        return aggregationBySourceBuilder(sourceBuilder, new AggregationResponseHandler());
    }

    public <R> Mono<R> aggregationBySourceBuilder(SearchSourceBuilder sourceBuilder, ResponseHandler<R> handler) {
        return Mono.create(sink -> {
            SearchRequest request = new SearchRequest(indexName);
            request.source(sourceBuilder);
            client.searchAsync(request, RequestOptions.DEFAULT, new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse response) {
                    sink.success(handler.handleResponse(response));
                }
                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }
    public Mono<Aggregations> aggregationByTemplate(String templateId, Map<String, Object> params) {
        return aggregationByTemplate(templateId, params, new AggregationResponseHandler());
    }
    public <R> Mono<R> aggregationByTemplate(String templateId, Map<String, Object> params, ResponseHandler<R> handler ) {
        return Mono.create(sink -> {
            SearchTemplateRequest request = new SearchTemplateRequest(new SearchRequest(indexName));
            request.setScriptType(ScriptType.STORED);
            request.setScript(templateId);
            request.setScriptParams(params);
            client.searchTemplateAsync(request, RequestOptions.DEFAULT, new ActionListener<SearchTemplateResponse>() {
                @Override
                public void onResponse(SearchTemplateResponse response) {
                    sink.success(handler.handleResponse(response.getResponse()));
                }
                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    public Mono<List<String>> suggestBySingleField(String suggestField, String prefixKey){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.suggest(new SuggestBuilder()
                .addSuggestion("mySuggestion",
                        SuggestBuilders.completionSuggestion(suggestField).prefix(prefixKey)
                                .size(30).skipDuplicates(true)));
        return suggestBySourceBuilder(sourceBuilder);
    }
    public Mono<List<String>> suggestBySourceBuilder(SearchSourceBuilder sourceBuilder){
        return suggestBySourceBuilder(sourceBuilder, new SuggestResponseHandler());
    }
    public <R> Mono<R> suggestBySourceBuilder(SearchSourceBuilder sourceBuilder, ResponseHandler<R> handler){
        return Mono.create(sink -> {
            SearchRequest request = new SearchRequest(indexName);
            request.source(sourceBuilder);
            client.searchAsync(request, RequestOptions.DEFAULT, new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse response) {
                    sink.success(handler.handleResponse(response));
                }
                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }
    public Mono<List<String>> suggestByTemplate(String templateId, Map<String, Object> params) {
        return suggestByTemplate(templateId, params, new SuggestResponseHandler());
    }
    public <R> Mono<R> suggestByTemplate(String templateId, Map<String, Object> params, ResponseHandler<R> handler) {
        return Mono.create(sink -> {
            SearchTemplateRequest request = new SearchTemplateRequest(new SearchRequest(indexName));
            request.setScriptType(ScriptType.STORED);
            request.setScript(templateId);
            request.setScriptParams(params);
            client.searchTemplateAsync(request, RequestOptions.DEFAULT, new ActionListener<SearchTemplateResponse>() {
                @Override
                public void onResponse(SearchTemplateResponse response) {
                    sink.success(handler.handleResponse(response.getResponse()));
                }

                @Override
                public void onFailure(Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    private String getID(T t) {
        if(t == null){
            throw new RuntimeException(t.getClass().getName() + "实例不能为null！");
        }
        try {
            Object value = idField.get(t);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            throw new RuntimeException("实体类中没有id字段或者id字段没有get方法");
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private T fromJson(String json) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // object 方法，走原生方法
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this,args);
        }
        // 其它走本地代理
        return method.invoke(this, args);
    }
}
