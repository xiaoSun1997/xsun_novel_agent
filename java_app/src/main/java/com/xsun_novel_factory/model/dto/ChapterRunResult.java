package com.xsun_novel_factory.model.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChapterRunResult {
    private Long draftId;
    private Long reviewId;
    private PublishResult publishResult;
}
