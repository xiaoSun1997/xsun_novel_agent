package com.xsun_novel_factory.service.impl;

import cn.hutool.json.JSONUtil;
import com.xsun_novel_factory.feign.GraphRagClient;
import com.xsun_novel_factory.model.dto.graph.*;
import com.xsun_novel_factory.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final GraphRagClient graphRagClient;

    @Override
    public GraphResponse updateKnowledgeGraph(String novelId, Integer chapterNo, String content) {
        ExtractGraphReq req = new ExtractGraphReq();
        req.setNovelId(novelId);
        req.setChapter(chapterNo);
        req.setContent(content);
        
        return graphRagClient.extractGraph(req);
    }

    @Override
    public GraphResponse queryKnowledgeGraph(String novelId, String queryType, String entityName, Integer chapter) {
        QueryGraphReq req = new QueryGraphReq();
        req.setNovelId(novelId);
        req.setQueryType(queryType);
        req.setEntityName(entityName);
        req.setChapter(chapter);
        
        return graphRagClient.queryGraph(req);
    }

    @Override
    public GraphResponse checkLogicConsistency(String novelId, Integer chapterNo, String content) {
        CheckLogicReq req = new CheckLogicReq();
        req.setNovelId(novelId);
        req.setChapter(chapterNo);
        req.setContent(content);
        String response1 = graphRagClient.checkLogic(req);
        GraphResponse response = JSONUtil.toBean(response1,GraphResponse.class);
        return response;
    }
}