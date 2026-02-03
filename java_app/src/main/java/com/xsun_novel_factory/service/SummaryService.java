package com.xsun_novel_factory.service;

import com.xsun_novel_factory.model.entity.Summary;

public interface SummaryService {
    /**
     * 生成并保存章节总结
     * @param novelId 小说ID
     * @param startChapter 起始章节
     * @param endChapter 结束章节
     * @param summaryContent 总结内容
     * @return 保存的总结实体
     */
    Summary generateAndSaveSummary(Long novelId, int startChapter, int endChapter, String summaryContent);
    
    /**
     * 获取最新总结
     * @param novelId 小说ID
     * @return 最新总结内容
     */
    String getLatestSummary(Long novelId);
}