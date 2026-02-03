package com.xsun_novel_factory.service;

import com.xsun_novel_factory.model.dto.graph.GraphResponse;

public interface KnowledgeGraphService {
    /**
     * 从章节内容中提取实体和关系并更新知识图谱
     * @param novelId 小说ID
     * @param chapterNo 章节号
     * @param content 章节内容
     * @return 提取结果
     */
    GraphResponse updateKnowledgeGraph(String novelId, Integer chapterNo, String content);
    
    /**
     * 查询知识图谱中的实体关系
     * @param novelId 小说ID
     * @param queryType 查询类型
     * @param entityName 实体名称
     * @param chapter 章节号
     * @return 查询结果
     */
    GraphResponse queryKnowledgeGraph(String novelId, String queryType, String entityName, Integer chapter);
    
    /**
     * 检查章节内容的逻辑一致性
     * @param novelId 小说ID
     * @param chapterNo 章节号
     * @param content 章节内容
     * @return 检查结果
     */
    GraphResponse checkLogicConsistency(String novelId, Integer chapterNo, String content);
}