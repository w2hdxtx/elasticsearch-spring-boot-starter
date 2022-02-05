package com.leyou.starter.elastic.repository;

import com.leyou.starter.elastic.entity.PageInfo;
import com.leyou.starter.elastic.handler.DataResponseHandler;
import com.leyou.starter.elastic.handler.PageHighlightResponseHandler;
import com.leyou.starter.elastic.handler.PageResponseHandler;
import com.leyou.starter.elastic.handler.ResponseHandler;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 定义了操作Elasticsearch的CRUD的功能 <br/>
 * 泛型说明 <br/>
 * T：实体类类型
 * ID：实体类中的id类型
 *
 */
public interface Repository<T, ID> {
    /**
     * 创建索引库
     *
     * @param source setting和mapping的json字符串
     * @return 是否创建成功
     */
    Boolean createIndex(String source);

    /**
     * 删除当前实体类相关的索引库
     *
     * @return 是否删除成功
     */
    public Boolean deleteIndex();

    /**
     * 新增数据
     *
     * @param t 要新增的数据
     * @return 是否新增成功
     */
    boolean save(T t);

    /**
     * 批量新增
     *
     * @param iterable 要新增的数据结婚
     * @return 是否新增成功
     */
    boolean saveAll(Iterable<T> iterable);

    /**
     * 根据id删除数据
     *
     * @param id id
     * @return 是否删除成功
     */
    boolean deleteById(ID id);

    /**
     * 异步功能，根据id查询数据
     *
     * @param id id
     * @return 包含实体类的Mono实例
     */
    Mono<T> queryById(ID id);

    /**
     * 根据{@link SearchSourceBuilder}查询数据，通过自定义{@link DataResponseHandler}来处理响应的Response，返回值自己定义.
     *
     * @param sourceBuilder 查询条件构建器
     * @param handler       结果处理器
     * @param <R>           结果处理器处理后的返回值的类型
     * @return 结果处理器处理后的的数据
     */
    <R> Mono<R> queryBySourceBuilder(SearchSourceBuilder sourceBuilder, DataResponseHandler<R, T> handler);

    /**
     * 根据{@link SearchSourceBuilder}查询数据，并使用了默认的{@link PageResponseHandler}，
     * 返回分页结果{@link PageInfo}
     *
     * @param sourceBuilder 查询条件构建器
     * @return 结果处理器处理后的的数据
     */
    Mono<PageInfo<T>> queryBySourceBuilderForPage(SearchSourceBuilder sourceBuilder);

    /**
     * 根据{@link SearchSourceBuilder}查询数据，并使用了默认的{@link PageHighlightResponseHandler}，
     * 返回分页结果{@link PageInfo}，其中的数据已经高亮处理
     *
     * @param sourceBuilder 查询条件构建器
     * @return 结果处理器处理后的的数据
     */
    Mono<PageInfo<T>> queryBySourceBuilderForPageHighlight(SearchSourceBuilder sourceBuilder);

    /**
     * 根据指定的templateId查询数据，需要使用自定义的{@link DataResponseHandler}处理响应数据，
     * 返回值由{@link DataResponseHandler}控制
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @param handler    结果处理器
     * @param <R>        结果处理器处理后的返回值的类型
     * @return 结果处理器处理后的数据
     */
    <R> Mono<R> queryByTemplate(String templateId, Map<String, Object> params, DataResponseHandler<R, T> handler);

    /**
     * 根据指定的templateId查询数据，使用默认的{@link PageResponseHandler}处理响应数据，
     * 返回值是分页结果{@link PageInfo}控制
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @return 结果处理器处理后的分页数据
     */
    Mono<PageInfo<T>> queryByTemplateForPage(String templateId, Map<String, Object> params);
    /**
     * 根据指定的templateId查询数据，并使用了默认的{@link PageHighlightResponseHandler}，
     * 返回分页结果{@link PageInfo}，其中的数据已经高亮处理
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @return 结果处理器处理后的{@link PageInfo}数据，已经高亮处理
     */
    Mono<PageInfo<T>> queryByTemplateForPageHighlight(String templateId, Map<String, Object> params);

    /**
     * 根据{@link SearchSourceBuilder}做数据聚合查询，返回原始的聚合结果{@link Aggregations}，没有做解析
     *
     * @param sourceBuilder 查询条件构建器
     * @return 原始的聚合结果{@link Aggregations}
     */
    Mono<Aggregations> aggregationBySourceBuilder(SearchSourceBuilder sourceBuilder);
    /**
     * 根据{@link SearchSourceBuilder}做数据聚合查询，返回原始的聚合结果{@link Aggregations}，使用指定的{@link ResponseHandler} 来解析
     *
     * @param sourceBuilder 查询条件构建器
     * @return 原始的聚合结果{@link Aggregations}
     */
    <R> Mono<R> aggregationBySourceBuilder(SearchSourceBuilder sourceBuilder, ResponseHandler<R> handler);
    /**
     * 根据指定的templateId查询聚合结果，返回原始的聚合结果{@link Aggregations}，没有做解析
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @return 原始的聚合结果{@link Aggregations}
     */
    Mono<Aggregations> aggregationByTemplate(String templateId, Map<String, Object> params);
    /**
     * 根据指定的templateId查询聚合结果，返回原始的聚合结果{@link Aggregations}，使用指定的{@link ResponseHandler} 来解析
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @return 原始的聚合结果{@link Aggregations}
     */
    <R> Mono<R> aggregationByTemplate(String templateId, Map<String, Object> params, ResponseHandler<R> handler );

    /**
     * 根据指定的prefixKey对单个指定suggestField 做自动补全，返回推荐结果的列表{@link List}
     * @param suggestField 补全字段
     * @param prefixKey 关键字
     * @return 返回推荐结果列表{@link List}
     */
    Mono<List<String>> suggestBySingleField(String suggestField, String prefixKey);
    /**
     * 根据{@link SearchSourceBuilder}做自动补全推荐，返回推荐结果的列表{@link List}
     * @param sourceBuilder 补全的条件
     * @return 返回推荐结果列表{@link List}
     */
    Mono<List<String>> suggestBySourceBuilder(SearchSourceBuilder sourceBuilder);
    /**
     * 根据{@link SearchSourceBuilder}做自动补全推荐，推荐结果由指定的{@link ResponseHandler} 来解析
     * @param sourceBuilder 补全的条件
     * @param handler 结果处理器
     * @return 返回推荐结果列表{@link List}
     */
    <R> Mono<R> suggestBySourceBuilder(SearchSourceBuilder sourceBuilder, ResponseHandler<R> handler);
    /**
     * 根据指定的templateId做completion suggest类型查询，返回推荐结果{@link List}
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @return 返回推荐结果{@link List}
     */
    Mono<List<String>> suggestByTemplate(String templateId, Map<String, Object> params);
    /**
     * 根据指定的templateId做completion suggest类型查询，需要使用自定义的{@link DataResponseHandler}处理响应数据，
     * 返回值由{@link DataResponseHandler}控制
     *
     * @param templateId 模板id
     * @param params     模板参数
     * @param handler    结果处理器
     * @param <R>        结果处理器处理后的返回值的类型
     * @return 原始的，未处理的推荐结果，由自定义的{@link DataResponseHandler}控制返回值
     */
    <R> Mono<R> suggestByTemplate(String templateId, Map<String, Object> params, ResponseHandler<R> handler);
}
