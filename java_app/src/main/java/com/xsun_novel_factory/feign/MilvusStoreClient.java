package com.xsun_novel_factory.feign;

import com.xsun_novel_factory.model.dto.milvus.MilvusResponse;
import com.xsun_novel_factory.model.dto.milvus.RetrieveReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Milvus 向量存储服务客户端
 * 对应 milvus_store 服务的 API 接口
 */
@FeignClient(name = "milvus-store-service", url = "${milvus.store.service.url:http://localhost:8001}")
public interface MilvusStoreClient {

    /**
     * 检查服务健康状态
     * 对应 /health 接口
     *
     * @return 服务健康状态信息
     */
    @GetMapping("/health")
    MilvusResponse health();

    /**
     * 上传作品文档到向量库
     * 对应 /works/upload 接口
     *
     * @param file 上传的文件
     * @param workId 作品ID
     * @param workTitle 作品标题
     * @param docType 文档类型：story/technique/notes/world_setting
     * @param tags 标签（可选）
     * @param chapter 章节（可选）
     * @return 上传结果信息
     */
    @PostMapping(value = "/works/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    MilvusResponse uploadWork(
            @RequestPart("file") MultipartFile file,
            @RequestParam("workId") String workId,
            @RequestParam("workTitle") String workTitle,
            @RequestParam("docType") String docType,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "chapter", required = false) String chapter
    );

    /**
     * 执行RAG检索
     * 对应 /rag/retrieve 接口
     *
     * @param req 检索请求对象，包含问题列表、过滤条件和topK参数
     * @return 检索结果映射，键为问题，值为答案列表
     */
    @PostMapping("/rag/retrieve")
    Map<String, Object> ragRetrieve(@RequestBody RetrieveReq req);
}