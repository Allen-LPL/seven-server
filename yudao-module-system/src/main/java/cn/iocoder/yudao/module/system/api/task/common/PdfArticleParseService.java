package cn.iocoder.yudao.module.system.api.task.common;

import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.PdfParseService;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import com.google.common.collect.Lists;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PdfArticleParseService {

  @Resource
  private PdfParseService pdfParseService;

  @Resource
  private ArticleService articleService;


  /**
   * 异步解析PDF并更新文章信息
   */
  public void asyncParsePdfAndUpdate(ArticleDO articleDO) {
    pdfParseService.parsePdfAsync(articleDO.getFilePath())
        .thenAccept(parseResult -> {
          try {
            log.info("PDF解析结果: {}", parseResult);
            if (parseResult.getSuccess() != null && parseResult.getSuccess()) {
              // 解析成功，更新文章信息
              updateArticleWithPdfResult(articleDO, parseResult);
              log.info("PDF解析成功并更新文章信息: {}", articleDO.getFileName());
            } else {
              log.warn("PDF解析失败: {}, 错误信息: {}", articleDO.getFileName(), parseResult.getErrorMessage());
            }
          } catch (Exception e) {
            log.error("更新文章信息失败: {}", articleDO.getFileName(), e);
          }
        })
        .exceptionally(throwable -> {
          log.error("PDF异步解析异常: {}", articleDO.getFileName(), throwable);
          return null;
        });
  }

  /**
   * 根据PDF解析结果更新文章信息
   */
  public void updateArticleWithPdfResult(ArticleDO articleDO, PdfParseResultDTO parseResult) {
    try {
      ArticleDO updateArticle = new ArticleDO();
      updateArticle.setArticleId(articleDO.getArticleId());
      updateArticle.setId(articleDO.getId());

      pdfParseService.transArticleToPdf(articleDO, parseResult);

      // 执行更新
      log.info("更新文章信息: {}", updateArticle);
      articleService.update(updateArticle);

    } catch (Exception e) {
      log.error("更新文章信息失败", e);
      throw new RuntimeException("更新文章信息失败: " + e.getMessage());
    }
  }



}
