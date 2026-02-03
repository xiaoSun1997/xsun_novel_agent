package com.xsun_novel_factory.model.mapper;

import com.xsun_novel_factory.model.entity.Chapter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 孙令磊
* @description 针对表【chapter(正式章节表（仅存审查通过内容）)】的数据库操作Mapper
* @createDate 2026-01-02 15:31:03
* @Entity com.xsun_novel_factory.domain.Chapter
*/
public interface ChapterMapper extends BaseMapper<Chapter> {

    /**
     * 查询指定范围内的章节
     */
    List<Chapter> selectChaptersInRange(Long novelId, int startChapter, int endChapter);
}




