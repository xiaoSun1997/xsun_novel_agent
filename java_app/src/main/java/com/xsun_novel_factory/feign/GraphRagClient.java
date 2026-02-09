package com.xsun_novel_factory.feign;

import com.xsun_novel_factory.model.dto.graph.CheckLogicReq;
import com.xsun_novel_factory.model.dto.graph.ExtractGraphReq;
import com.xsun_novel_factory.model.dto.graph.GraphResponse;
import com.xsun_novel_factory.model.dto.graph.QueryGraphReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * GraphRAG 知识图谱服务客户端
 * 对应 graph_rag_neo4j 服务的 API 接口
 */
@FeignClient(name = "graph-rag-service", url = "${graph.rag.service.url:http://localhost:8000}")
public interface GraphRagClient {

    /**
     * 从章节内容中提取实体和关系并存入 Neo4j
     * 对应 /graph/extract 接口
     *
     * @param req 提取请求对象，包含小说ID、章节号和章节正文内容
     * @return 图谱提取结果响应
     */
    @PostMapping("/graph/extract")
    GraphResponse extractGraph(@RequestBody ExtractGraphReq req);

    /**
     * 查询知识图谱信息
     * 支持查询类型：character(人物)/location(地点)/organization(组织)/timeline(时间线)
     * 对应 /graph/query 接口
     *
     * @param req 查询请求对象，包含小说ID、查询类型等参数
     * @return 图谱查询结果响应
     */
    @PostMapping("/graph/query")
    GraphResponse queryGraph(@RequestBody QueryGraphReq req);

    /**
     * 检查新章节内容是否存在逻辑错误
     * 对应 /graph/check-logic 接口
     *
     * @param req 逻辑检查请求对象，包含小说ID、章节号和待检查内容
     * @return 逻辑检查结果响应
     */
    @PostMapping("/graph/check-logic")
    String checkLogic(@RequestBody CheckLogicReq req);
}