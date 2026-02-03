package com.xsun_novel_factory.agent;

import com.xsun_novel_factory.model.dto.ChapterRunResult;
import com.xsun_novel_factory.model.dto.NovelCreationRequest;
import com.xsun_novel_factory.model.entity.Novel;

public interface ChiefCoordinator {

    /**
     * 创建新小说
     * @return 新创建的小说对象
     */
    Novel createNewNovel(Integer novelId);

    /**
     * 生成并审查章节
     * @param novelId 小说ID
     * @param chapterNo 章节号
     * @param createdBy 创建者
     * @return 章节运行结果
     */
    ChapterRunResult generateAndReviewChapter(Long novelId, int chapterNo, String createdBy);

    /**
     * 创建新小说（带参数）
     * @param request 小说创建请求
     * @return 新创建的小说对象
     */
    Novel createNewNovelWithParams(NovelCreationRequest request);
}