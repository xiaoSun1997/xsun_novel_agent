package com.xsun_novel_factory.service;

import com.xsun_novel_factory.model.entity.OutboxEvent;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 孙令磊
* @description 针对表【outbox_event(Outbox事件表（可靠异步通知GraphRAG）)】的数据库操作Service
* @createDate 2026-01-02 15:31:03
*/
public interface OutboxEventService extends IService<OutboxEvent> {

}
