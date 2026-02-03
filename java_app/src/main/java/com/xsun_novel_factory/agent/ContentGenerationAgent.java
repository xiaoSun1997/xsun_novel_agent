package com.xsun_novel_factory.agent;

import com.xsun_novel_factory.model.dto.DraftContent;
import com.xsun_novel_factory.model.dto.NovelDto;

public interface ContentGenerationAgent {

    /**
     * 生成草稿
     *
     * @param novelId   小说id
     * @param chapterNo 章节编码
     * @return 草稿内容
     */
    DraftContent generateDraft(Long novelId, int chapterNo, String roleGraphResponse, String locationGraphResponse, String timeLineGraphResponse);

}
