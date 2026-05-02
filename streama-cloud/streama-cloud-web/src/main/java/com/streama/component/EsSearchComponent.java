package com.streama.component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import com.streama.entity.config.AppConfig;
import com.streama.entity.dto.VideoInfoEsDto;
import com.streama.entity.po.UserInfo;
import com.streama.entity.po.VideoInfo;
import com.streama.entity.query.UserInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.SearchOrderTypeEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.UserInfoMapper;
import com.streama.utils.CopyTools;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("esSearchComponent")
@Slf4j
public class EsSearchComponent {

    @Resource
    private AppConfig appConfig;

    @Resource
    private ElasticsearchClient elasticsearchClient;  // 改为新客户端

    @Resource
    private UserInfoMapper userInfoMapper;

    /**
     * 检查索引是否存在
     */
    private Boolean isExistIndex() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e
                    .index(appConfig.getEsIndexVideoName())
            );
            return elasticsearchClient.indices().exists(existsRequest).value();
        } catch (Exception e) {
            log.error("检查索引是否存在失败", e);
            return false;
        }
    }

    /**
     * 创建索引
     */
    public void createIndex() throws BusinessException {
        try {
            // 检查索引是否已存在
            if (isExistIndex()) {
                log.info("索引 {} 已存在，跳过创建", appConfig.getEsIndexVideoName());
                return;
            }

            // 构建创建索引请求
            CreateIndexRequest createIndexRequest = CreateIndexRequest.of(c -> c
                    .index(appConfig.getEsIndexVideoName())
                    .settings(s -> s
                            .analysis(a -> a
                                    .analyzer("comma", an -> an
                                            .pattern(p -> p
                                                    .pattern(",")
                                            )
                                    )
                            )
                    )
                    .mappings(m -> m
                            .properties("videoId", p -> p
                                    .text(t -> t.index(false))
                            )
                            .properties("userId", p -> p
                                    .text(t -> t.index(false))
                            )
                            .properties("videoCover", p -> p
                                    .text(t -> t.index(false))
                            )
                            .properties("videoName", p -> p
                                    .text(t -> t.analyzer("ik_max_word"))
                            )
                            .properties("tags", p -> p
                                    .text(t -> t.analyzer("comma"))
                            )
                            .properties("playCount", p -> p
                                    .integer(i -> i.index(false))
                            )
                            .properties("danmuCount", p -> p
                                    .integer(i -> i.index(false))
                            )
                            .properties("collectCount", p -> p
                                    .integer(i -> i.index(false))
                            )
                            .properties("createTime", p -> p
                                    .date(d -> d
                                            .format("yyyy-MM-dd HH:mm:ss")
                                            .index(false)
                                    )
                            )
                    )
            );

            // 执行创建索引
            CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(createIndexRequest);
            Boolean acknowledged = createIndexResponse.acknowledged();

            if (!acknowledged) {
                throw new BusinessException("初始化es失败");
            }

            log.info("索引 {} 创建成功", appConfig.getEsIndexVideoName());

        } catch (Exception e) {
            log.error("初始化es失败", e);
            throw new BusinessException("初始化es失败: " + e.getMessage());
        }
    }

    public void saveDoc(VideoInfo videoInfo) throws BusinessException {
        try {
            if(docExist(videoInfo.getVideoId())){
                updateDoc(videoInfo);
            }

            VideoInfoEsDto videoInfoEsDto = CopyTools.copy(videoInfo,VideoInfoEsDto.class);
            videoInfoEsDto.setCollectCount(0);
            videoInfoEsDto.setPlayCount(0);
            videoInfoEsDto.setDanmuCount(0);
            IndexRequest<VideoInfoEsDto> request = IndexRequest.of(i -> i
                    .index(appConfig.getEsIndexVideoName())
                    .id(videoInfoEsDto.getVideoId())
                    .document(videoInfoEsDto)  // 直接传入对象，不需要手动转 JSON
            );
            elasticsearchClient.index(request);
        } catch (Exception e) {
            log.error("保存到es失败", e);
            throw new BusinessException("保存到es失败");
        }
    }

    private Boolean docExist(String id) throws IOException {
        GetRequest getRequest = GetRequest.of(g -> g
                .index(appConfig.getEsIndexVideoName())
                .id(id)
        );
        GetResponse<Void> response = elasticsearchClient.get(getRequest, Void.class);
        return response.found();
    }

    private void updateDoc(VideoInfo videoInfo) throws BusinessException {
        try {
            videoInfo.setLastUpdateTime(null);
            videoInfo.setCreateTime(null);

            Map<String, Object> dataMap = new HashMap<>();
            Field[] fields = videoInfo.getClass().getDeclaredFields();
            for(Field field : fields){
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = videoInfo.getClass().getMethod(methodName);
                Object object = method.invoke(videoInfo);

                if(object!=null && object instanceof String && !StringTools.isEmpty(object.toString()) || object != null && !(object instanceof String)){
                    dataMap.put(field.getName(), object);
                }
            }
            if(dataMap.isEmpty()) {
                return;
            }
            UpdateRequest<Map, Map> updateRequest = UpdateRequest.of(u -> u
                    .index(appConfig.getEsIndexVideoName())
                    .id(videoInfo.getVideoId())
                    .doc(dataMap)
            );

            elasticsearchClient.update(updateRequest, Map.class);
        } catch (Exception e) {
            log.error("es更新视频失败", e);
            throw new BusinessException("保存视频失败");
        }
    }

    public void updateDocCount(String videoId, String fieldName, Integer count) throws BusinessException {
        try {
//            Script script = new Script(ScriptType.INLINE, "painless", "ctx._source." + fieldName + " += params.count", Collections.singletonMap("count", count));
            Script script = Script.of(s -> s
                    .inline(inline -> inline
                            .source("ctx._source." + fieldName + " += params.count")
                            .params("count", JsonData.of(count))
                    )
            );
            UpdateRequest updateRequest = UpdateRequest.of(u -> u
                    .index(appConfig.getEsIndexVideoName())
                    .id(videoId)
                    .script(script)
            );
            elasticsearchClient.update(updateRequest, Void.class);
        } catch (Exception e) {
            log.error("更新数量到es失败", e);
            throw new BusinessException("更新数量到es失败");
        }
    }

    public void deleteDoc(String videoId) throws BusinessException {
        try {
            DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                    .index(appConfig.getEsIndexVideoName())
                    .id(videoId)
            );
            elasticsearchClient.delete(deleteRequest);
        } catch (Exception e) {
            log.error("删除视频失败", e);
            throw new BusinessException("删除视频失败");
        }
    }

    public PaginationResultVO<VideoInfo> search(Boolean highlight, String keyword, Integer orderType, Integer pageNo, Integer pageSize) throws BusinessException {
        try {
            SearchOrderTypeEnum searchOrderTypeEnum = SearchOrderTypeEnum.getByType(orderType);

            List<Query> mustQueries = new ArrayList<>();

            // 多字段匹配查询
            if (!StringTools.isEmpty(keyword)) {
                mustQueries.add(
                        MultiMatchQuery.of(m -> m
                                .fields("videoName", "tags")
                                .query(keyword)
                        )._toQuery()
                );
            }

            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.of(s -> {
                s.index(appConfig.getEsIndexVideoName());

                // 设置查询
                if (!mustQueries.isEmpty()) {
                    s.query(q -> q.bool(b -> b.must(mustQueries)));
                }

                // 设置分页
                int currentPage = pageNo == null ? 1 : pageNo;
                int currentSize = pageSize == null ? PageSize.SIZE20.getSize() : pageSize;
                s.from((currentPage - 1) * currentSize);
                s.size(currentSize);

                // 设置排序
                s.sort(sort -> sort.score(score -> score.order(SortOrder.Asc)));
                if (orderType != null && searchOrderTypeEnum != null) {
                    s.sort(sort -> sort
                            .field(field -> field
                                    .field(searchOrderTypeEnum.getField())
                                    .order(SortOrder.Desc)
                            )
                    );
                }

                // 设置高亮
                if (highlight != null && highlight) {
                    s.highlight(h -> h
                            .fields("videoName", hf -> hf
                                    .preTags("<span class='highlight'>")
                                    .postTags("</span>")
                            )
                    );
                }

                return s;
            });

            // 执行搜索
            SearchResponse<VideoInfo> searchResponse = elasticsearchClient.search(searchRequest, VideoInfo.class);

            // 处理结果
            long totalCount = searchResponse.hits().total() != null ?
                    searchResponse.hits().total().value() : 0;

            List<VideoInfo> videoInfoList = new ArrayList<>();
            List<String> userIdList = new ArrayList<>();

            if (searchResponse.hits().hits() != null) {
                for (Hit<VideoInfo> hit : searchResponse.hits().hits()) {
                    VideoInfo videoInfo = hit.source();
                    if (videoInfo != null) {
                        // 处理高亮
                        if (hit.highlight() != null && hit.highlight().containsKey("videoName")) {
                            List<String> highlights = hit.highlight().get("videoName");
                            if (highlights != null && !highlights.isEmpty()) {
                                videoInfo.setVideoName(highlights.get(0));
                            }
                        }
                        videoInfoList.add(videoInfo);
                        userIdList.add(videoInfo.getUserId());
                    }
                }
            }

            // 获取用户信息
            if (!userIdList.isEmpty()) {
                UserInfoQuery userInfoQuery = new UserInfoQuery();
                userInfoQuery.setUserIdList(userIdList);
                List<UserInfo> userInfoList = userInfoMapper.selectList(userInfoQuery);
                Map<String, UserInfo> userInfoMap = userInfoList.stream()
                        .collect(Collectors.toMap(UserInfo::getUserId, Function.identity(), (v1, v2) -> v2));

                // 设置昵称
                videoInfoList.forEach(item -> {
                    UserInfo userInfo = userInfoMap.get(item.getUserId());
                    item.setNickName(userInfo==null?"":userInfo.getNickName());
                });
            }

            // 构建分页结果
            int currentPage = pageNo == null ? 1 : pageNo;
            int currentSize = pageSize == null ? PageSize.SIZE20.getSize() : pageSize;
            int pageTotal = (int) Math.ceil((double) totalCount / currentSize);

            PaginationResultVO<VideoInfo> resultVO = new PaginationResultVO<>(
                    (int) totalCount,
                    currentSize,
                    currentPage,
                    pageTotal,
                    videoInfoList
            );

            return resultVO;

        } catch(Exception e) {
            log.error("es搜索视频失败", e);
            throw new BusinessException("查询失败");
        }
    }
}