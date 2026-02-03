package com.xsun_novel_factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xsun_novel_factory.model.entity.OutboxEvent;
import com.xsun_novel_factory.model.mapper.OutboxEventMapper;
import com.xsun_novel_factory.service.OutboxEventService;
import org.springframework.stereotype.Service;

/**
* @author 孙令磊
* @description 针对表【outbox_event(Outbox事件表（可靠异步通知GraphRAG）)】的数据库操作Service实现
* @createDate 2026-01-02 15:31:03
*/
@Service
public class OutboxEventServiceImpl extends ServiceImpl<OutboxEventMapper, OutboxEvent>
    implements OutboxEventService {

}




