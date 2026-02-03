package com.xsun_novel_factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xsun_novel_factory.model.entity.Chapter;
import com.xsun_novel_factory.model.mapper.ChapterMapper;
import com.xsun_novel_factory.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;

    @Override
    public List<Chapter> getChaptersInRange(Long novelId, int startChapter, int endChapter) {
        return chapterMapper.selectChaptersInRange(novelId, startChapter, endChapter);
    }

    @Override
    public Chapter findById(Long id) {
        return chapterMapper.selectById(id);
    }

    @Override
    public Integer findLatestChapter(Integer novelId) {
        final Chapter chapter = chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getNovelId, novelId)
                .orderByDesc(Chapter::getChapterNo)
                .last("limit 1")
                .select());
        return chapter == null ? 1 : chapter.getChapterNo()+1;
    }
}