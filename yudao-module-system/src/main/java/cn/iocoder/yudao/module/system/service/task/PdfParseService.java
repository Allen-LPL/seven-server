package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PdfParseService {

    @Resource
    private RestTemplate restTemplate;

    private static final String PDF_PARSE_URL = "http://socket.weilantech.com/upload";

    /**
     * 异步解析PDF文件
     *
     * @param filePath PDF文件路径
     * @return 解析结果
     */
    @Async("taskExecutor")
    public CompletableFuture<PdfParseResultDTO> parsePdfAsync(String filePath) {
        try {
            log.info("开始异步解析PDF文件：{}", filePath);
            
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("PDF文件不存在：{}", filePath);
                return CompletableFuture.completedFuture(createErrorResult("文件不存在"));
            }

            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            ResponseEntity<PdfParseResultDTO> response = restTemplate.postForEntity(
                    PDF_PARSE_URL, requestEntity, PdfParseResultDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PdfParseResultDTO result = response.getBody();
                log.info("PDF解析成功：{}", filePath);
                return CompletableFuture.completedFuture(result);
            } else {
                log.error("PDF解析失败，HTTP状态码：{}", response.getStatusCode());
                return CompletableFuture.completedFuture(createErrorResult("解析失败"));
            }

        } catch (Exception e) {
            log.error("PDF解析异常：{}", filePath, e);
            return CompletableFuture.completedFuture(createErrorResult("解析异常：" + e.getMessage()));
        }
    }

    /**
     * 创建错误结果
     */
    private PdfParseResultDTO createErrorResult(String errorMsg) {
        PdfParseResultDTO result = new PdfParseResultDTO();
        result.setSuccess(false);
        result.setErrorMessage(errorMsg);
        return result;
    }

    public PdfParseResultDTO parsePdf(String filePath){
        try {
            log.info("start parsePdf , filePath: {}", filePath);
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("PDF文件不存在：{}", filePath);
                return null;
            }

            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            ResponseEntity<PdfParseResultDTO> response = restTemplate.postForEntity(
                PDF_PARSE_URL, requestEntity, PdfParseResultDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PdfParseResultDTO result = response.getBody();
                log.info("parsePdf PDF解析成功：{}", filePath);
                return result;
            } else {
                log.error("parsePdf PDF解析失败，HTTP状态码：{}， filePath : {}", response.getStatusCode(), filePath);
                return null;
            }

        }catch (Exception e){
            log.error("filePath: {}, parsePdf error", filePath, e);
            return null;
        }
    }

    public void transArticleToPdf(ArticleDO updateArticle, PdfParseResultDTO parseResult) {
        // 更新文章标题
        if (parseResult.getTitle() != null) {
            updateArticle.setArticleTitle(parseResult.getTitle().substring(Math.min(1000, parseResult.getTitle().length())));
        }

        // 更新杂志名称
        if (parseResult.getJournal() != null) {
            updateArticle.setArticleJournal(parseResult.getJournal());
        }

        // 更新关键词列表
        if (parseResult.getKeywords() != null && !parseResult.getKeywords().isEmpty()) {
            updateArticle.setArticleKeywords(parseResult.getKeywords());
        }

        // 更新作者姓名列表
        if (parseResult.getAuthors() != null && !parseResult.getAuthors().isEmpty()) {
            updateArticle.setAuthorName(parseResult.getAuthors());
            // 由于API没有返回作者单位信息，暂时设置为空列表
            updateArticle.setAuthorInstitution(Lists.newArrayList());
        }

        // 更新发表日期
        if (parseResult.getPublicationDate() != null) {
            try {
                // 假设日期格式为 yyyy-MM-dd，需要转换为时间戳
                java.time.LocalDate date = java.time.LocalDate.parse(parseResult.getPublicationDate());
                updateArticle.setArticleDate(date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
            } catch (Exception e) {
                log.warn("解析发表日期失败: {}", parseResult.getPublicationDate());
            }
        }else {
            try {
                updateArticle.setArticleDate(0L);
            }catch (Exception e){
                log.error("error: ",e);
            }
        }

        // 更新DOI
        if (parseResult.getDoi() != null) {
            updateArticle.setPmid(parseResult.getDoi().substring(0,Math.min(29,parseResult.getDoi().length()))); // 暂时将DOI存储在PMID字段
        }
    }
} 
