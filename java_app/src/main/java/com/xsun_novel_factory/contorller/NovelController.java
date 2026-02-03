package com.xsun_novel_factory.contorller;

import com.xsun_novel_factory.agent.ChiefCoordinator;
import com.xsun_novel_factory.agent.graph.ChapterProductionGraph;
import com.xsun_novel_factory.agent.state.ChapterFlowState;
import com.xsun_novel_factory.model.dto.ChapterRunResult;
import com.xsun_novel_factory.model.dto.NovelCreationRequest;
import com.xsun_novel_factory.model.dto.NovelIdReq;
import com.xsun_novel_factory.model.entity.Novel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/novel")
@RequiredArgsConstructor
public class NovelController {

    private final ChiefCoordinator chiefCoordinator;

    private final ChapterProductionGraph chapterProductionGraph;

    @PostMapping("/{novelId}/chapter/{chapterNo}/run")
    public ChapterRunResult run(@PathVariable Long novelId,
                                @PathVariable int chapterNo,
                                @RequestParam(defaultValue = "system") String createdBy) {
        return chiefCoordinator.generateAndReviewChapter(novelId, chapterNo, createdBy);
    }

    @PostMapping("/init")
    public Novel run() {
        return chiefCoordinator.createNewNovel(null);
    }
    
    @PostMapping("/init-with-params")
    public Novel createNovelWithParams(@RequestBody NovelCreationRequest request) {
        return chiefCoordinator.createNewNovelWithParams(request);
    }

    @SneakyThrows
    @PostMapping("/init-with-novelId")
    public ChapterFlowState createNovelWithId(@RequestBody NovelIdReq request) {
        return chapterProductionGraph.createNovel(request.getNovelId());
    }
}