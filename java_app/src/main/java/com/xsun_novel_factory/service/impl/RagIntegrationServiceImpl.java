package com.xsun_novel_factory.service.impl;

import cn.hutool.json.JSONUtil;
import com.xsun_novel_factory.feign.GraphRagClient;
import com.xsun_novel_factory.feign.MilvusStoreClient;
import com.xsun_novel_factory.model.dto.graph.CheckLogicReq;
import com.xsun_novel_factory.model.dto.graph.ExtractGraphReq;
import com.xsun_novel_factory.model.dto.graph.GraphResponse;
import com.xsun_novel_factory.model.dto.graph.QueryGraphReq;
import com.xsun_novel_factory.model.dto.milvus.MilvusResponse;
import com.xsun_novel_factory.model.dto.milvus.RetrieveReq;
import com.xsun_novel_factory.service.RagIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * RAG 集成服务实现类
 * 集成了 GraphRAG 和 Milvus 向量存储服务
 */
@Service
@RequiredArgsConstructor
public class RagIntegrationServiceImpl implements RagIntegrationService {

    private final GraphRagClient graphRagClient;
    private final MilvusStoreClient milvusStoreClient;

    /**
     * 从章节内容中提取实体和关系并存入 Neo4j
     *
     * @param novelId 小说ID
     * @param chapter 章节号
     * @param content 章节正文内容
     * @return 图谱提取结果
     */
    @Override
    public GraphResponse extractGraphToKnowledgeBase(String novelId, Integer chapter, String content) {
        ExtractGraphReq req = new ExtractGraphReq();
        req.setNovelId(novelId);
        req.setChapter(chapter);
        req.setContent(content);
        
        return graphRagClient.extractGraph(req);
    }

    /**
     * 查询知识图谱信息
     *
     * @param novelId   小说ID
     * @param queryType 查询类型：character(人物)/location(地点)/organization(组织)/timeline(时间线)
     * @param entityName 实体名称（可选，不填返回全部）
     * @param chapter   限制章节范围（可选）
     * @return 图谱查询结果
     */
    @Override
    public GraphResponse queryKnowledgeGraph(String novelId, String queryType, String entityName, Integer chapter) {
        QueryGraphReq req = new QueryGraphReq();
        req.setNovelId(novelId);
        req.setQueryType(queryType);
        req.setEntityName(entityName);
        req.setChapter(chapter);
        
        return graphRagClient.queryGraph(req);
    }

    /**
     * 检查新章节内容是否存在逻辑错误
     *
     * @param novelId 小说ID
     * @param chapter 当前章节号
     * @param content 待检查的正文内容
     * @return 逻辑检查结果
     */
    @Override
    public GraphResponse checkChapterLogic(String novelId, Integer chapter, String content) {
        CheckLogicReq req = new CheckLogicReq();
        req.setNovelId(novelId);
        req.setChapter(chapter);
        req.setContent(content);
        String response1 = graphRagClient.checkLogic(req);
        GraphResponse response = JSONUtil.toBean(response1,GraphResponse.class);
        return response;
    }

    /**
     * 上传作品文档到向量库
     *
     * @param file      上传的文件
     * @param workId    作品ID
     * @param workTitle 作品标题
     * @param docType   文档类型：story/technique/notes/world_setting
     * @param tags      标签（可选）
     * @param chapter   章节（可选）
     * @return 上传结果
     */
    @Override
    public MilvusResponse uploadWorkDocument(MultipartFile file, String workId, String workTitle, 
                                             String docType, String tags, String chapter) {
        return milvusStoreClient.uploadWork(file, workId, workTitle, docType, tags, chapter);
    }

    /**
     * 执行RAG检索
     *
     * @param req 检索请求对象
     * @return 检索结果映射
     */
    @Override
    public Map<String, Object> retrieveWithRag(RetrieveReq req) {
        return milvusStoreClient.ragRetrieve(req);
    }

    /**
     * 检查服务健康状态
     *
     * @return 服务健康状态
     */
    @Override
    public MilvusResponse checkServiceHealth() {
        return milvusStoreClient.health();
    }
}