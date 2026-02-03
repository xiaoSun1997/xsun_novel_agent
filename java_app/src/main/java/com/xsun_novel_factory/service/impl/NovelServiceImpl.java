package com.xsun_novel_factory.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xsun_novel_factory.model.dto.NovelDto;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.model.mapper.NovelMapper;
import com.xsun_novel_factory.service.NovelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 孙令磊
 * @description 针对表【novel(小说主表（作品维度的元数据）)】的数据库操作Service实现
 * @createDate 2026-01-02 15:31:03
 */
@Service
public class NovelServiceImpl extends ServiceImpl<NovelMapper, Novel>
        implements NovelService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Novel createNovel(NovelDto novelDto) {
        final Novel novel = new Novel();
        novel.setTitle(novelDto.getTitle())
                .setDescription(novelDto.getDescription())
                .setVolumes(JSONUtil.toJsonStr(novelDto.getVolumes()));
        this.save(novel);
        return novel;
    }
}




