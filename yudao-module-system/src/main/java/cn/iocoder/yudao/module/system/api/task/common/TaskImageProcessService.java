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
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.FileTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.TaskStatusEnum;
import cn.iocoder.yudao.module.system.enums.task.VectorQueryTypeEnum;
import cn.iocoder.yudao.module.system.service.notify.NotifySendService;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgReportService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.PdfParseService;
import cn.iocoder.yudao.module.system.service.task.QueryFeaturePointService;
import cn.iocoder.yudao.module.system.service.task.QueryImageTypeService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import cn.iocoder.yudao.module.system.service.task.VectorQueryService;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

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

  @Resource
  private TaskConfig taskConfig;

  @Resource
  private Executor taskExecutor;

  @Resource
  private QueryFeaturePointService queryFeaturePointService;

  @Resource
  private VectorQueryService vectorQueryService;

  @Resource
  private QueryImageTypeService queryImageTypeService;

  @Resource
  private NotifySendService notifySendService;

  public void processAsync(Long taskId){
    CompletableFuture.runAsync(() -> {
      process(taskId);
    }, taskExecutor);
  }

  public void process(Long taskId) {
    Stopwatch stopwatch = Stopwatch.createStarted();
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
    log.info("processAsync【1/10】start parse pdf, taskId = {}", taskId);
    List<ArticleDO> articleDOList  = articleService.queryListByTaskId(taskId);
    if (FileTypeEnum.PDF.getCode().equals(fileType)){
      for (ArticleDO articleDO : articleDOList) {
        log.info("start parse pdf articleDO = {}", JSONObject.toJSONString(articleDO));
        PdfParseResultDTO pdfParseResultDTO = pdfParseService.parsePdf(articleDO.getFilePath());
        if (pdfParseResultDTO == null) {
          pdfParseResultDTO = new PdfParseResultDTO();
          pdfParseResultDTO.setPublicationDate(String.valueOf(System.currentTimeMillis()));
        }
        log.info("end parse pdf, pdfParseResultDTO = {}", JSONObject.toJSONString(pdfParseResultDTO));
        pdfParseService.transArticleToPdf(articleDO, pdfParseResultDTO);
      }
      log.info("articleDOList : {}",JSONObject.toJSONString(articleDOList));
      Boolean flag = articleService.updateBatch(articleDOList);
      log.info("end parse pdf, flag = {}", flag);
    }
    log.info("processAsync【1/10】end parse pdf, taskId = {}, article size = {}, take {}ms", taskId, articleDOList.size(),
        stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 2.调py接口：切割大图小图 & 小图向量化
    log.info("processAsync【2/10】start cut image, taskId = {}", taskId);
    List<ProcessImageRequest> request = ImageBeanTransUtils.getProcessImageRequests(taskId, articleDOList,taskConfig.getReplacePrefix(),
        FilePathConstant.local_prefix, FilePathConstant.LARGE_PATH, FilePathConstant.SMALL_PATH, FilePathConstant.PREVIEW_PATH);
    String response = HttpUtils.post(taskConfig.getProcessImageUrl(),null, JSONObject.toJSONString(request));
    Optional<String> resultStr = ImageBeanTransUtils.getImageCutResultStr(taskId, request, response);
    if (!resultStr.isPresent()) {
      return;
    }
    log.info("processAsync【2/10】end cut image, taskId = {}, take {}ms", taskId,stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 3.将大图小图写入数据库
    log.info("processAsync【3/10】start insert image, taskId = {}", taskId);
    List<SmallImageDO> allSmallList = Lists.newArrayList();
    int largeCount = 0, smallCount = 0;
    List<ProcessImageResponse> responseList = JSONObject.parseArray(resultStr.get(), ProcessImageResponse.class);
    for (ProcessImageResponse processImageResponse : responseList) {
      Long articleId = processImageResponse.getArticleId();
      List<LargeImage> largeImageList = processImageResponse.getLargeImageList();
      for (LargeImage largeImage : largeImageList) {
        LargeImageDO largeImageDO = ImageBeanTransUtils.transLargeImageDO(largeImage,articleId,taskConfig.getReplacePrefix(),
            FilePathConstant.local_prefix);
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
          SmallImageDO smallImageDO = ImageBeanTransUtils.transSmallImageDO(smallImage,articleId, largeImageDO.getId(),
              taskConfig.getReplacePrefix(), FilePathConstant.local_prefix);
          smallImageDO.setCreator(String.valueOf(userId));
          smallImageDO.setIsSource(0);
          smallImageDOList.add(smallImageDO);
          smallCount++;
        }
        if (CollectionUtils.isNotEmpty(smallImageDOList)) {
          Boolean flag = smallImageService.batchSave(smallImageDOList);
          if (!flag) {
            log.error("smallImageService.batchSave error, smallImageDOList = {}", JSONObject.toJSONString(smallImageDOList));
          }
        }
        largeCount++;
        allSmallList.addAll(smallImageDOList);
      }
    }
    log.info("processAsync【3/10】end insert image, taskId = {}, large count = {}, small count = {}, take {}ms",
        taskId, largeCount, smallCount, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 4.向量检索
    log.info("processAsync【4/10】start milvus query, taskId = {}", taskId);
    VectorQueryTypeEnum queryType = VectorQueryTypeEnum.COSINE;
    List<ImgSimilarityDO> recallList = vectorQueryService.query(allSmallList, taskId, queryType,taskType,imageTaskDO.getStrategyConfig());
    Set<Long> smallImageIdSet= recallList.stream().map(ImgSimilarityDO::getTargetSmallImageId).collect(Collectors.toSet());
    log.info("processAsync【4/10】end milvus query, taskId = {}, recall size = {}, take {}ms",
        taskId, recallList.size(),stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 5.补充相似图片的 文章id 和 大图id
    log.info("processAsync【5/10】start complete image info, taskId = {}", taskId);
    if(CollectionUtils.isNotEmpty(smallImageIdSet)) {
      List<SmallImageDO> smallImageDOList = smallImageService.queryByIds(smallImageIdSet);
      Map<Long,SmallImageDO> smallImageDOMap = smallImageDOList.stream().collect(Collectors.toMap(SmallImageDO::getId, x -> x));
      for (ImgSimilarityDO imgSimilarityDO : recallList) {
        SmallImageDO smallImageDO = smallImageDOMap.get(imgSimilarityDO.getTargetSmallImageId());
        if (Objects.nonNull(smallImageDO)) {
          imgSimilarityDO.setTargetArticleId(smallImageDO.getArticleId());
          imgSimilarityDO.setTargetLargeImageId(smallImageDO.getLargeImageId());
        }
        imgSimilarityDO.setCreator(String.valueOf(userId));
        imgSimilarityDO.setUpdater(String.valueOf(userId));
      }
    }
    log.info("processAsync【5/10】end complete image info, taskId = {}, take {}ms", taskId, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 7.写入相似图片对
    log.info("processAsync【7/10】start insert similar image, taskId = {}", taskId);
    if (CollectionUtils.isNotEmpty(recallList)){
      List<ImgSimilarityDO> batchList = Lists.newArrayList();
      for (int i = 0; i < recallList.size(); i++) {
        batchList.add(recallList.get(i));
        if (i%100 == 0 && i>0){
          Boolean flag = imgSimilarityService.batchInsert(batchList);
          if (!flag){
            log.error("批量写入相似图片对失败");
          }
          batchList.clear();
        }
      }
      if (CollectionUtils.isNotEmpty(batchList)){
        imgSimilarityService.batchInsert(batchList);
      }
    }
    log.info("processAsync【7/10】end insert similar image, taskId = {}, similar size = {}, take {}ms",
        taskId, recallList.size(),stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 8.查询特征点
    log.info("processAsync【8/10】start query feature points, taskId = {}", taskId);
    queryFeaturePointService.queryFeaturePoints(recallList,taskId);
    log.info("processAsync【8/10】end query feature points, taskId = {}, take {}ms", taskId, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 9.查询图片类型
    log.info("processAsync【9/10】start query image type, taskId = {}", taskId);
    //queryImageTypeService.queryImageType(allSmallList,taskId);
    log.info("processAsync【9/10】end query image type, taskId = {}, take {}ms", taskId, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 10.更新任务状态为专家审核
    log.info("processAsync【10/10】start update task status, taskId = {}", taskId);
    ImageTaskDO updateImageTaskStatus = new ImageTaskDO();
    updateImageTaskStatus.setId(taskId);
    updateImageTaskStatus.setTaskStatus(TaskStatusEnum.EXPERT_REVIEW.getCode());
    imageTaskService.update(updateImageTaskStatus);
    log.info("processAsync【10/10】end update task status, taskId = {}, take {}ms", taskId,stopwatch.elapsed(TimeUnit.MILLISECONDS));

    log.info("end process async, taskId = {}, fileType = {}, take {} ms", taskId, fileType, stopwatch.elapsed(TimeUnit.MILLISECONDS));


    // 发送站内信
    sendNotify(taskId,1L);

    stopwatch.stop();
  }

  // 发送站内信
  private void sendNotify(Long taskId, Long userId) {
    AdminUserDO adminUserDO = adminUserService.getUser(userId);
    String templateCode = "algorithmDoneToReviewer";
    Map<String, Object> templateParams = new HashMap<>();
    templateParams.put("userName", adminUserDO.getNickname());
    templateParams.put("taskNo", taskId);
    notifySendService.sendSingleNotifyToAdmin(userId, templateCode, templateParams);
  }

  private String getReportName(){
    try {
      AdminUserDO userDO = adminUserService.getUser(WebFrameworkUtils.getLoginUserId());
      if (Objects.nonNull(userDO)){
        String userName = userDO.getUsername();
        String date = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        return StrUtil.format("{}_{}",userName,date);
      }
    }catch (Exception e){
      log.error("getReportName error: ",e);
    }
    return UUID.randomUUID().toString();
  }

}
