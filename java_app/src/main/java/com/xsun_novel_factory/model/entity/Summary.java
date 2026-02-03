package com.xsun_novel_factory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 总结
 * @TableName summary
 */
@TableName(value ="summary")
@Data
public class Summary {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属小说ID
     */
    private Long novelId;

    /**
     * 章节范围（如：1-10表示第1章到第10章的总结）
     */
    private String chapterRange;

    /**
     * 章节正文内容（最终发布版本）
     */
    private String content;
}