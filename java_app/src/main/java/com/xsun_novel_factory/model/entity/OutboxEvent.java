package com.xsun_novel_factory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * Outbox事件表（可靠异步通知GraphRAG）
 * @TableName outbox_event
 */
@TableName(value ="outbox_event")
@Data
public class OutboxEvent {
    /**
     * 事件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 聚合根类型（如CHAPTER）
     */
    private String aggregateType;

    /**
     * 聚合根ID（如chapter.id）
     */
    private Long aggregateId;

    /**
     * 事件类型（如CHAPTER_APPROVED）
     */
    private String eventType;

    /**
     * 事件负载（推送Python所需数据）
     */
    private Object payloadJson;

    /**
     * 事件状态（NEW/SENT/FAILED）
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    private Date nextRetryAt;

    /**
     * 失败原因记录
     */
    private String errorMessage;

    /**
     * 幂等键（防止重复消费）
     */
    private String idempotencyKey;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}