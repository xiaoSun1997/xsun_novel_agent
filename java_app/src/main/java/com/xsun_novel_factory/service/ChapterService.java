package com.xsun_novel_factory.service;

import com.xsun_novel_factory.model.entity.Chapter;

import java.util.List;

public interface ChapterService {
    /**
     * 获取指定范围内的章节
     * @param novelId 小说ID
     * @param startChapter 起始章节号
     * @param endChapter 结束章节号
     * @return 章节列表
     */
    List<Chapter> getChaptersInRange(Long novelId, int startChapter, int endChapter);
    
    /**
     * 根据ID获取章节
     * @param id 章节ID
     * @return 章节实体
     */
    Chapter findById(Long id);

    /**
     * 查询最新章节
     * @param novelId
     * @return
     */
    Integer findLatestChapter(Integer novelId);
}