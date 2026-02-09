package com.xsun_novel_factory.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.xsun_novel_factory.agent.ai.WriteAi;
import com.xsun_novel_factory.model.dto.DraftContent;
import com.xsun_novel_factory.model.dto.NovelDto;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.model.entity.Chapter;
import com.xsun_novel_factory.service.NovelService;
import com.xsun_novel_factory.service.ChapterService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentGenerationAgentImpl implements ContentGenerationAgent {
    private final ChatModel chatModel;
    private final NovelService novelService;
    private final ChapterService chapterService;

    @Override
    public DraftContent generateDraft(Long novelId, int chapterNo, String roleGraphResponse, String locationGraphResponse, String timeLineGraphResponse) {
        // 获取小说大纲信息
        Novel novel = novelService.getById(novelId);
        final NovelDto novelDto = new NovelDto();
        novelDto.setTitle(novel.getTitle()).setDescription(novel.getDescription());
        List<NovelDto.Volume> list = JSONUtil.toList(novel.getVolumes(), NovelDto.Volume.class);
        novelDto.setVolumes(list);
        String outLine = novel.getDescription();
        // 根据章节编号查询前5章内容
        String beforeChapterInfo = getPreviousChaptersContent(novelId, chapterNo);
        
        WriteAi writeAi = AiServices.builder(WriteAi.class)
                .chatModel(chatModel)
                .build();
        String content = writeAi.writeChapter(novelId, chapterNo, outLine, novel.getVolumes(), beforeChapterInfo,roleGraphResponse,locationGraphResponse,timeLineGraphResponse);

        String traceId = IdUtil.fastSimpleUUID();

        return DraftContent.builder()
                .title("第 " + chapterNo + " 章")
                .content(content)
                // metaJson 第一版先简单：你后续可以补"摘要/人物出场/事件列表"
                .metaJson("{\"outline\":\"stub\",\"traceId\":\"" + traceId + "\"}")
                .traceId(traceId)
                .build();
    }
    
    /**
     * 根据章节编号获取对应卷的细纲
     */
    private String getVolumeDetailLine(String volumesJson, int chapterNo) {
        // 这里需要解析volumesJson并根据章节编号确定是第几卷
        // 简单实现，实际需要解析JSON并计算卷信息
        int volumeNo = ((chapterNo - 1) / 20) + 1; // 假设每20章为一卷
        return "第" + volumeNo + "卷细纲内容";
    }
    
    /**
     * 获取前面章节的内容
     */
    private String getPreviousChaptersContent(Long novelId, int currentChapterNo) {
        // 查询前5章的内容
        int startChapter = Math.max(1, currentChapterNo - 1);
        int endChapter = currentChapterNo - 1;
        
        if (endChapter < 1) {
            return ""; // 如果是第一章，则没有前置章节
        }
        
        List<Chapter> previousChapters = chapterService.getChaptersInRange(novelId, startChapter, endChapter);
        
        if (CollUtil.isEmpty(previousChapters)) {
            return "";
        }
        
        return previousChapters.stream()
                .sorted((c1, c2) -> Integer.compare(c1.getChapterNo(), c2.getChapterNo()))
                .map(chapter -> "第" + chapter.getChapterNo() + "章:\n" + chapter.getContent())
                .collect(Collectors.joining("\n\n"));
    }
}
