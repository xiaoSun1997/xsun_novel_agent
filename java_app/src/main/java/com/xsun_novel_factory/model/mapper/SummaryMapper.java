package com.xsun_novel_factory.model.mapper;

import com.xsun_novel_factory.model.entity.Summary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author sunlinglei
 * {@code @description} 针对表【summary(总结)】的数据库操作Mapper
 * {@code @createDate} 2026-01-08 19:34:35
 * {@code @Entity} com.xsun_novel_factory.model.entity.Summary
 */
public interface SummaryMapper extends BaseMapper<Summary> {
    
    /**
     * 根据小说ID和章节范围查询总结
     */
    Summary selectByChapterRange(Long novelId, int startChapter, int endChapter);
    
    /**
     * 根据小说ID查询最新总结
     */
    Summary selectLatestByNovelId(Long novelId);
}




