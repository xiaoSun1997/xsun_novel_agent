package com.xsun_novel_factory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 小说主表（作品维度的元数据）
 * @author sunlinglei
 * @TableName novel
 */
@TableName(value ="novel")
@Data
@Accessors(chain = true)
public class Novel {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 小说标题
     */
    private String title;

    /**
     * 小说简介/总体设定
     */
    private String description;

    /**
     * 卷/卷细纲
     */
    private String volumes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}