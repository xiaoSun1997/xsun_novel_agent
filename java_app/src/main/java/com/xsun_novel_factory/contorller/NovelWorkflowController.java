package com.xsun_novel_factory.contorller;

import com.xsun_novel_factory.model.dto.ChapterRunResult;
import com.xsun_novel_factory.model.dto.NovelCreationRequest;
import com.xsun_novel_factory.model.entity.Novel;
import com.xsun_novel_factory.service.NovelCreationWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小说创作工作流控制器
 * 提供完整的创作流程API
 */
@RestController
@RequestMapping("/novel-workflow")
@RequiredArgsConstructor
public class NovelWorkflowController {

    private final NovelCreationWorkflowService novelCreationWorkflowService;

    /**
     * 创建小说（完整流程）
     * 1. 从Milvus检索参考资料
     * 2. 生成大纲
     * 3. 等待用户确认
     * 4. 开始章节创作循环
     */
    @PostMapping("/create-full")
    public Novel createNovelWithFullWorkflow(@RequestBody NovelCreationRequest request) {
        return novelCreationWorkflowService.createNovelWithFullWorkflow(request);
    }

    /**
     * 写作章节（带完整检查流程）
     * 1. 查询人物关系图谱
     * 2. 生成章节
     * 3. GraphRAG一致性检查
     * 4. 质量审查
     * 5. 用户确认
     * 6. 发布章节
     * 7. 更新知识图谱
     * 8. 检查完成状态
     */
    @PostMapping("/{novelId}/chapter/{chapterNo}/write-full")
    public ChapterRunResult writeChapterWithFullWorkflow(
            @PathVariable Long novelId,
            @PathVariable int chapterNo,
            @RequestParam(defaultValue = "system") String createdBy) {
        return novelCreationWorkflowService.writeChapterWithFullWorkflow(novelId, chapterNo, createdBy);
    }
}