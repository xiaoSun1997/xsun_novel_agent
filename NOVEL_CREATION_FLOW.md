# 小说创作流程详解

## 完整创作流程图

```mermaid
graph TD
    A[开始创作] --> B[指定小说类型和参考文献]
    B --> C[Milvus检索参考资料]
    C --> D[AI生成小说大纲]
    D --> E{用户确认大纲?}
    E -->|否| D
    E -->|是| F[开始章节创作]
    F --> G[查询人物关系图谱]
    G --> H[生成章节草稿]
    H --> I[GraphRAG一致性检查]
    I --> J{一致性检查通过?}
    J -->|否| H
    J -->|是| K[质量审查]
    K --> L[用户确认章节]
    L --> M{用户确认?}
    M -->|否| H
    M -->|是| N[发布章节]
    N --> O[更新知识图谱]
    O --> P[检查是否卷末/书末]
    P --> Q{是否卷末?}
    Q -->|是| R[生成卷总结]
    Q -->|否| S{是否书末?}
    S -->|是| T[完结作品]
    S -->|否| U{是否需要阶段性总结?}
    U -->|是| R
    U -->|否| V[继续下一章]
    R --> V
    V --> F
    T --> W[结束]
    
    style A fill:#e1f5fe
    style W fill:#e8f5e8
    style E fill:#fff3e0
    style L fill:#fff3e0
    style J fill:#fff3e0
    style M fill:#fff3e0
    style Q fill:#fff3e0
    style S fill:#fff3e0
    style U fill:#fff3e0
```

## 关键节点说明

### 1. 参考资料检索
- 从 Milvus 向量数据库检索相关写作理论和参考小说
- 提取有用的写作技巧、故事结构、人物塑造方法等

### 2. 大纲生成与确认
- AI 结合检索结果生成小说大纲
- 用户确认机制确保创作方向符合预期

### 3. 章节创作循环
- 每章都经过多重检查确保质量
- 人物关系图谱确保角色一致性
- GraphRAG 确保逻辑一致性

### 4. 质量控制
- AI 自动审查
- 用户手动确认
- 逻辑一致性检查

### 5. 知识管理
- 知识图谱持续更新
- 阶段性总结维护连贯性
- 人物关系图谱动态维护
```

## API 调用时序

```mermaid
sequenceDiagram
    participant U as 用户
    participant JA as Java App
    participant MS as Milvus Store
    participant GR as GraphRAG
    participant AI as AI Models
    
    U->>JA: 创建小说请求 (指定类型/参考文献)
    JA->>MS: 检索参考资料
    MS-->>JA: 返回TopK检索结果
    JA->>AI: 生成大纲
    AI-->>JA: 大纲内容
    JA-->>U: 显示大纲，等待确认
    U->>JA: 确认大纲
    loop 章节创作
        JA->>GR: 查询人物关系图谱
        GR-->>JA: 人物关系数据
        JA->>AI: 生成章节草稿
        AI-->>JA: 章节内容
        JA->>GR: 逻辑一致性检查
        GR-->>JA: 检查结果
        alt 一致性检查通过
            JA->>AI: 质量审查
            AI-->>JA: 审查结果
            JA-->>U: 显示章节，等待确认
            U->>JA: 确认章节
            JA->>JA: 发布章节
            JA->>GR: 更新知识图谱
        else 一致性检查失败
            JA->>AI: 重新生成章节
        end
    end
```