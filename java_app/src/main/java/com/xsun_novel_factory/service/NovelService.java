package com.xsun_novel_factory.service;

import com.xsun_novel_factory.model.dto.NovelDto;
import com.xsun_novel_factory.model.entity.Novel;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 孙令磊
* @description 针对表【novel(小说主表（作品维度的元数据）)】的数据库操作Service
* @createDate 2026-01-02 15:31:03
*/
public interface NovelService extends IService<Novel> {

    /**
     * init
     * @param novelDto
     * @return
     */
    Novel createNovel(NovelDto novelDto);

}
