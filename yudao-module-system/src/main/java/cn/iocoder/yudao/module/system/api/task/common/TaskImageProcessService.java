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
    log.info("【1/10】start parse pdf, taskId = {}", taskId);
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
      articleService.updateBatch(articleDOList);
    }
    log.info("【1/10】end parse pdf, taskId = {}", taskId);

    // 2.调py接口：切割大图小图 & 小图向量化
    log.info("【2/10】start cut image, taskId = {}", taskId);
    List<ProcessImageRequest> request = ImageBeanTransUtils.getProcessImageRequests(taskId, articleDOList,taskConfig.getReplacePrefix(),
        FilePathConstant.local_prefix, FilePathConstant.LARGE_PATH, FilePathConstant.SMALL_PATH);
    String response = HttpUtils.post(taskConfig.getProcessImageUrl(),null, JSONObject.toJSONString(request));
    Optional<String> resultStr = ImageBeanTransUtils.getImageCutResultStr(taskId, request, response);
    if (!resultStr.isPresent()) {
      return;
    }
    log.info("【2/10】end cut image, taskId = {}", taskId);

    // 3.将大图小图写入数据库
    log.info("【3/10】start insert image, taskId = {}", taskId);
    List<SmallImageDO> allSmallList = Lists.newArrayList();
    List<LargeImageDO> allLargeList = Lists.newArrayList();
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
        }
        Boolean flag = smallImageService.batchSave(smallImageDOList);
        if (!flag) {
          log.error("aaa");
        }
        allLargeList.add(largeImageDO);
        allSmallList.addAll(smallImageDOList);
      }
    }
    log.info("【3/10】end insert image, taskId = {}", taskId);

    // 4.向量检索
    log.info("【4/10】start milvus query, taskId = {}", taskId);
    VectorQueryTypeEnum queryType = VectorQueryTypeEnum.COSINE;
    List<ImgSimilarityDO> recallList = vectorQueryService.query(allSmallList, taskId, queryType,taskType,imageTaskDO.getStrategyConfig());
    Set<Long> smallImageIdSet= recallList.stream().map(ImgSimilarityDO::getTargetSmallImageId).collect(Collectors.toSet());
    log.info("【4/10】end milvus query, taskId = {}", taskId);

    // 5.补充相似图片的 文章id 和 大图id
    log.info("【5/10】start complete image info, taskId = {}", taskId);
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
    log.info("【5/10】end complete image info, taskId = {}", taskId);

    // 6.写入报告
    log.info("【6/10】start insert report, taskId = {}", taskId);
    ImgReportDO imgReportDO = new ImgReportDO();
    imgReportDO.setTaskId(taskId);
    imgReportDO.setReportType(2);
    imgReportDO.setReportName(getReportName());
    imgReportDO.setCreator(String.valueOf(userId));
    imgReportDO.setUpdater(String.valueOf(userId));
    imgReportDO.setCreatorId(userId);
    imgReportService.insert(imgReportDO);
    log.info("【6/10】end insert report, taskId = {}", taskId);

    // 7.写入相似图片对
    log.info("【7/10】start insert similar image, taskId = {}", taskId);
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
    log.info("【7/10】end insert similar image, taskId = {}", taskId);

    // 8.查询特征点
    log.info("【8/10】start query feature points, taskId = {}", taskId);
    queryFeaturePointService.queryFeaturePoints(recallList);
    log.info("【8/10】end query feature points, taskId = {}", taskId);

    // 9.查询图片类型
    log.info("【9/10】start query image type, taskId = {}", taskId);
    //queryImageTypeService.queryImageType(allLargeList,allSmallList); todo
    log.info("【9/10】end query image type, taskId = {}", taskId);

    // 10.更新任务状态为专家审核
    log.info("【10/10】start update task status, taskId = {}", taskId);
    ImageTaskDO updateImageTaskStatus = new ImageTaskDO();
    updateImageTaskStatus.setId(taskId);
    updateImageTaskStatus.setTaskStatus(TaskStatusEnum.EXPERT_REVIEW.getCode());
    imageTaskService.update(updateImageTaskStatus);
    log.info("【10/10】end update task status, taskId = {}", taskId);

    log.info("end process async, taskId = {}, fileType = {}", taskId, fileType);
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
