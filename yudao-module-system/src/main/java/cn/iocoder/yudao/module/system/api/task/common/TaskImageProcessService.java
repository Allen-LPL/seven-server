package cn.iocoder.yudao.module.system.api.task.common;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageRequest;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage.SmallImage;
import cn.iocoder.yudao.module.system.api.task.utils.ImageBeanTransUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.FileTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.TaskStatusEnum;
import cn.iocoder.yudao.module.system.enums.task.VectorQueryTypeEnum;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgReportService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.PdfParseService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import cn.iocoder.yudao.module.system.service.task.VectorQueryService;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskImageProcessService {

  @Resource
  private ArticleService articleService;

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private ImgReportService imgReportService;

  @Resource
  private ImgSimilarityService imgSimilarityService;

  @Resource
  private AdminUserService adminUserService;

  @Resource
  private PdfParseService pdfParseService;

  @Resource
  private ImageTaskService imageTaskService;

  private static final String LARGE_PATH = "%s%s/largeImage/";
  private static final String SMALL_PATH = "%s%s/smallImage/";
  private static final String local_prefix = "./task-file/";

  private static final String DB_LARGE_PATH = "%sdb/%s/largeImage/";
  private static final String DB_SMALL_PATH = "%sdb/%s/smallImage/";


  private static final String url = "http://172.20.76.8:8086/process_articles";

  @Value("${image.replace.prefix}")
  private String replacePrefix;

  @Resource
  private Executor taskExecutor;

  @Resource
  private VectorQueryService vectorQueryService;

  public void processAsync(Long taskId){
    CompletableFuture.runAsync(() -> {
      process(taskId);
    }, taskExecutor);
  }

  public void process(Long taskId) {
    log.info("start process async, taskId = {}", taskId);

    // 获取当前用户
    Long userId = WebFrameworkUtils.getLoginUserId();
    if (Objects.isNull(userId)) {
      userId = 1L;
    }

    // 获取当前task
    ImageTaskDO imageTaskDO = imageTaskService.getById(taskId);
    if (Objects.isNull(imageTaskDO)) {
      log.error("taskId = {} ,不存在", taskId);
    }
    String fileType = imageTaskDO.getFileType();
    Integer taskType = imageTaskDO.getTaskType();

    // 1.pdf解析
    List<ArticleDO> articleDOList  = articleService.queryListByTaskId(taskId);
    if (FileTypeEnum.PDF.getCode().equals(fileType)){
      for (ArticleDO articleDO : articleDOList) {
        log.info("start process async, articleDO = {}", JSONObject.toJSONString(articleDO));
        PdfParseResultDTO pdfParseResultDTO = pdfParseService.parsePdf(articleDO.getFilePath());
        if (pdfParseResultDTO == null) {
          pdfParseResultDTO = new PdfParseResultDTO();
          pdfParseResultDTO.setPublicationDate(String.valueOf(System.currentTimeMillis()));
        }
        log.info("end process async, pdfParseResultDTO = {}", JSONObject.toJSONString(pdfParseResultDTO));
        pdfParseService.transArticleToPdf(articleDO, pdfParseResultDTO);
      }
      articleService.updateBatch(articleDOList);
    }


    // 2.调py接口：切割大图小图 & 小图向量化
    List<ProcessImageRequest> request = ImageBeanTransUtils.getProcessImageRequests(taskId, articleDOList,replacePrefix,local_prefix,
        LARGE_PATH, SMALL_PATH);
    String response = HttpUtils.post(url,null, JSONObject.toJSONString(request));
    Optional<String> resultStr = ImageBeanTransUtils.getImageCutResultStr(taskId, request, response);
    if (!resultStr.isPresent()) {
      return;
    }

    // 2.将大图小图写入数据库
    List<SmallImageDO> allSmallList = Lists.newArrayList();
    List<ProcessImageResponse> responseList = JSONObject.parseArray(resultStr.get(), ProcessImageResponse.class);
    for (ProcessImageResponse processImageResponse : responseList) {
      Long articleId = processImageResponse.getArticleId();
      List<LargeImage> largeImageList = processImageResponse.getLargeImageList();
      for (LargeImage largeImage : largeImageList) {
        LargeImageDO largeImageDO = ImageBeanTransUtils.transLargeImageDO(largeImage,articleId,replacePrefix,local_prefix);
        largeImageDO.setCreator(String.valueOf(userId));
        largeImageDO.setIsSource(0);
        Integer number = largeImageService.insert(largeImageDO);
        if (Objects.isNull(number) || number <= 0) {
          log.error("写入失败");
          continue;
        }

        List<SmallImageDO> smallImageDOList = Lists.newArrayList();
        List<SmallImage> smallImageList = largeImage.getSmallImageList();
        for (SmallImage smallImage : smallImageList) {
          SmallImageDO smallImageDO = ImageBeanTransUtils.transSmallImageDO(smallImage,articleId, largeImageDO.getId(),replacePrefix,local_prefix);
          smallImageDO.setCreator(String.valueOf(userId));
          smallImageDO.setIsSource(0);
          smallImageDOList.add(smallImageDO);
        }
        Boolean flag = smallImageService.batchSave(smallImageDOList);
        if (!flag) {
          log.error("aaa");
        }
        allSmallList.addAll(smallImageDOList);
      }
    }

    // 向量检索
    VectorQueryTypeEnum queryType = VectorQueryTypeEnum.COSINE;
    List<ImgSimilarityDO> recallList = vectorQueryService.query(allSmallList, taskId, queryType,taskType,imageTaskDO.getStrategyConfig());
    Set<Long> smallImageIdSet= recallList.stream().map(ImgSimilarityDO::getTargetSmallImageId).collect(Collectors.toSet());

    // 补充相似图片的 文章id 和 大图id
    if(CollectionUtils.isNotEmpty(smallImageIdSet)) {
      List<SmallImageDO> smallImageDOList = smallImageService.queryByIds(smallImageIdSet);
      Map<Long,SmallImageDO> smallImageDOMap = smallImageDOList.stream().collect(Collectors.toMap(SmallImageDO::getId, x -> x));
      for (ImgSimilarityDO imgSimilarityDO : recallList) {
        SmallImageDO smallImageDO = smallImageDOMap.get(imgSimilarityDO.getTargetSmallImageId());
        if (Objects.nonNull(smallImageDO)) {
          imgSimilarityDO.setTargetArticleId(smallImageDO.getArticleId());
          imgSimilarityDO.setTargetLargeImageId(smallImageDO.getLargeImageId());
        }
      }
    }

    // 重排 todo 后面做


    // 写入报告
    ImgReportDO imgReportDO = new ImgReportDO();
    imgReportDO.setTaskId(taskId);
    imgReportDO.setReportType(2);
    imgReportDO.setReportName(getReportName());
    imgReportDO.setCreator(String.valueOf(userId));
    imgReportDO.setUpdater(String.valueOf(userId));
    imgReportDO.setCreatorId(userId);
    imgReportService.insert(imgReportDO);

    // 写入相似图片对
    if (CollectionUtils.isNotEmpty(recallList)){
      Boolean flag = imgSimilarityService.batchInsert(recallList);
      if (!flag){
        log.error("批量写入相似图片对失败");
      }
    }

    // 更新任务状态为算法检测中
    ImageTaskDO updateImageTaskStatus = new ImageTaskDO();
    updateImageTaskStatus.setId(taskId);
    updateImageTaskStatus.setTaskStatus(TaskStatusEnum.EXPERT_REVIEW.getCode());
    imageTaskService.update(updateImageTaskStatus);

    log.info("end process async, taskId = {}, fileType = {}", taskId, fileType);
  }



  private String getReportName(){
    try {
      AdminUserDO userDO = adminUserService.getUser(WebFrameworkUtils.getLoginUserId());
      String userName = userDO.getUsername();
      String date = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
      return StrUtil.format("{}_{}",userName,date);
    }catch (Exception e){
      log.error("getReportName error: ",e);
    }
    return UUID.randomUUID().toString();
  }




}
