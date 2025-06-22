package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
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
} 