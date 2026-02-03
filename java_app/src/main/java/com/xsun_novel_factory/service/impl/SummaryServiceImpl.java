package com.xsun_novel_factory.service.impl;

import com.xsun_novel_factory.agent.ai.SummaryAi;
import com.xsun_novel_factory.model.entity.Summary;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.model.entity.Chapter;
import com.xsun_novel_factory.model.mapper.SummaryMapper;
import com.xsun_novel_factory.service.NovelService;
import com.xsun_novel_factory.service.ChapterService;
import com.xsun_novel_factory.service.SummaryService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {
    
    private final SummaryMapper summaryMapper;
    private final NovelService novelService;
    private final ChapterService chapterService;
    private final ChatModel chatModel;
    
    @Override
    public Summary generateAndSaveSummary(Long novelId, int startChapter, int endChapter, String summaryContent) {
        // 检查是否已经存在相同范围的总结
        Summary existingSummary = summaryMapper.selectByChapterRange(novelId, startChapter, endChapter);
        if (existingSummary != null) {
            // 如果已存在，更新它
            existingSummary.setContent(summaryContent);
            summaryMapper.updateById(existingSummary);
            return existingSummary;
        }
        
        // 创建新的总结
        Summary summary = new Summary();
        summary.setNovelId(novelId);
        summary.setChapterRange(startChapter + "-" + endChapter);
        summary.setContent(summaryContent);
        
        summaryMapper.insert(summary);
        return summary;
    }
    
    @Override
    public String getLatestSummary(Long novelId) {
        // 获取最新的总结
        Summary latestSummary = summaryMapper.selectLatestByNovelId(novelId);
        if (latestSummary != null) {
            return latestSummary.getContent();
        }
        
        // 如果没有找到总结，尝试从AI生成
        return generateSummaryFromAI(novelId);
    }
    
    /**
     * 从AI生成总结
     */
    private String generateSummaryFromAI(Long novelId) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) {
            return "";
        }
        
        // 获取最近的章节内容
        List<Chapter> recentChapters = chapterService.getChaptersInRange(novelId, 1, 5); // 获取前5章
        
        if (recentChapters.isEmpty()) {
            return "";
        }
        
        // 将章节内容合并
        String chaptersContent = recentChapters.stream()
                .sorted((c1, c2) -> Integer.compare(c1.getChapterNo(), c2.getChapterNo()))
                .map(chapter -> "第" + chapter.getChapterNo() + "章:\n" + chapter.getContent())
                .collect(Collectors.joining("\n\n"));
        
        // 使用AI生成总结
        SummaryAi summaryAi = AiServices.builder(SummaryAi.class)
                .chatModel(chatModel)
                .build();
        
        try {
            String summary = summaryAi.generateChapterSummary(
                    novel.getTitle(),
                    1, // 当前分卷
                    "", // 历史总结
                    0, // 上次总结结束章节
                    1, // 起始章节
                    recentChapters.size(), // 结束章节
                    chaptersContent, // 增量内容
                    "生成简洁的章节总结，突出核心情节点和人物状态变化"
            );
            
            return summary;
        } catch (Exception e) {
            // 如果AI生成失败，返回默认总结
            return "暂无总结";
        }
    }
}