package com.xsun_novel_factory.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @program: NovelDto.java
 * @description: novel info
 * @author: sunmouren
 * @create: 2026-01-06
 **/
@Getter
@Setter
@Accessors(chain = true)
public class NovelDto implements Serializable {

    private String title;

    private String description;

    private List<Volume> volumes;

    @Getter
    @Setter
    public static class Volume implements Serializable {

        private String volumeNo;

        private String description;
    }
}