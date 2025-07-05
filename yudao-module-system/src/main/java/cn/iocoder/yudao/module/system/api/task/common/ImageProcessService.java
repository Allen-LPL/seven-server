package cn.iocoder.yudao.module.system.api.task.common;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageRequest;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage.SmallImage;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.MilvusConstant;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgReportService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.MilvusRecallService;
import cn.iocoder.yudao.module.system.service.task.PdfParseService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImageProcessService {

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
  private MilvusRecallService milvusRecallService;

  private static final String LARGE_PATH = "/Users/fangliu/Code/image_similar/seven-server/task-file/%s/largeImage/";
  private static final String SMALL_PATH = "/Users/fangliu/Code/image_similar/seven-server/task-file/%s/smallImage/";
  private static final String LARGE_PATH_DEV = "./task-file/%s/largeImage/";
  private static final String SMALL_PATH_DEV = "./task-file/%s/smallImage/";

  private static final String local_prefix = "./task-file/";
  private static final String local_prefix_replace = "/Users/fangliu/Code/image_similar/seven-server/task-file/";

  private static final String DB_LARGE_PATH = "/Users/fangliu/Code/image_similar/seven-server/task-file/db/%s/largeImage/";
  private static final String DB_SMALL_PATH = "/Users/fangliu/Code/image_similar/seven-server/task-file/db/%s/smallImage/";
  private static final String DB_LARGE_PATH_DEV = "./task-file/db/%s/largeImage/";
  private static final String DB_SMALL_PATH_DEV = "./task-file/db/%s/smallImage/";


  private static final String url = "http://localhost:8086/process_articles";

  @Resource
  private Executor taskExecutor;

  public void processAsync(Long taskId){
    CompletableFuture.runAsync(() -> {
      process(taskId);
    }, taskExecutor);
  }

  public void process(Long taskId) {

    Long userId = WebFrameworkUtils.getLoginUserId();
    if (Objects.isNull(userId)) {
      userId = 1L;
    }

    // 1.pdf解析
    List<ArticleDO> articleDOList  = articleService.queryListByTaskId(taskId);
    for (ArticleDO articleDO : articleDOList) {
      PdfParseResultDTO pdfParseResultDTO = pdfParseService.parsePdf(articleDO.getFilePath());
      if (pdfParseResultDTO == null) {
        pdfParseResultDTO = new PdfParseResultDTO();
        pdfParseResultDTO.setPublicationDate(String.valueOf(System.currentTimeMillis()));
      }
      pdfParseService.transArticleToPdf(articleDO, pdfParseResultDTO);
    }
    articleService.updateBatch(articleDOList);

    // 2.调py接口：切割大图小图 & 小图向量化
    List<ProcessImageRequest> request = Lists.newArrayList();
    for (ArticleDO articleDO : articleDOList) {
      ProcessImageRequest imageRequest = new ProcessImageRequest();
      imageRequest.setArticleId(articleDO.getId());
      if (SpringUtils.isLocal()){
        imageRequest.setFilePath(articleDO.getFilePath().replace(local_prefix,local_prefix_replace));
      }else {
        imageRequest.setFilePath(articleDO.getFilePath());
      }
      imageRequest.setFileType(articleDO.getFileType());
      if (SpringUtils.isLocal()){
        imageRequest.setLargePrefixPath(String.format(LARGE_PATH, taskId));
        imageRequest.setSmallPrefixPath(String.format(SMALL_PATH, taskId));
      }else {
        imageRequest.setLargePrefixPath(String.format(LARGE_PATH_DEV, taskId));
        imageRequest.setSmallPrefixPath(String.format(SMALL_PATH_DEV, taskId));
      }
      request.add(imageRequest);
    }
    String response = HttpUtils.post(url,null, JSONObject.toJSONString(request));
    log.info(response);
    if (StringUtils.isBlank(response)){
      log.error("任务失败，无返回，{}",taskId); // todo 写库
      return;
    }
    JSONObject jsonObject = JSONObject.parseObject(response);
    if (!"0000".equals(jsonObject.getString("code")) ) {
      log.error("任务失败,taskId={}, result : {}", taskId, jsonObject.getString("code"));
      return;
    }

    String resultStr = jsonObject.getString("data");
    if (StringUtils.isBlank(resultStr)) {
      log.error("任务失败,taskId={}, result : {}", taskId, jsonObject.getString("code"));
      return;
    }

    // 2.将大图小图写入数据库
    List<SmallImageDO> allSmallList = Lists.newArrayList();
    List<ProcessImageResponse> responseList = JSONObject.parseArray(resultStr, ProcessImageResponse.class);
    for (ProcessImageResponse processImageResponse : responseList) {
      Long articleId = processImageResponse.getArticleId();
      List<LargeImage> largeImageList = processImageResponse.getLargeImageList();
      for (LargeImage largeImage : largeImageList) {
        LargeImageDO largeImageDO = transLargeImageDO(largeImage,articleId);
        largeImageDO.setCreator(String.valueOf(userId));
        Integer number = largeImageService.insert(largeImageDO);
        if (Objects.isNull(number) || number <= 0) {
          log.error("写入失败");
          continue;
        }

        List<SmallImageDO> smallImageDOList = Lists.newArrayList();
        List<SmallImage> smallImageList = largeImage.getSmallImageList();
        for (SmallImage smallImage : smallImageList) {
          SmallImageDO smallImageDO = transSmallImageDO(smallImage,articleId, largeImageDO.getId());
          smallImageDO.setCreator(String.valueOf(userId));
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
    List<ImgSimilarityDO> recallList = Lists.newArrayList();
    Set<Long> smallImageIdList = Sets.newHashSet();
    for (SmallImageDO smallImageDO : allSmallList) {
      String vectorPath = smallImageDO.getVectorPath();
      Map<String,List<Float>> vectorMap = getVector(vectorPath);
      // todo  每个模型都检索一次，或者选一个检索一次
      for(String modelName : vectorMap.keySet()) {
        if ("Resnet50".equals(modelName)) {
          List<Map<String,Object>> resultList = milvusRecallService.recall(vectorMap.get(modelName), modelName);
          for (Map<String,Object> imageIdScoreMap : resultList) {
            ImgSimilarityDO imgSimilarityDO = new ImgSimilarityDO();
            imgSimilarityDO.setTaskId(taskId);
            imgSimilarityDO.setAlgorithmName(modelName);
            imgSimilarityDO.setSourceArticleId(smallImageDO.getArticleId());
            imgSimilarityDO.setSourceLargeImageId(smallImageDO.getLargeImageId());
            imgSimilarityDO.setSourceSmallImageId(smallImageDO.getId());
            imgSimilarityDO.setSimilarityScore(MapUtils.getDoubleValue(imageIdScoreMap, "score",0.0f));
            Long targetSmallImageId = MapUtils.getLong(imageIdScoreMap, MilvusConstant.imageId,0L);
            imgSimilarityDO.setTargetSmallImageId(targetSmallImageId);
            imgSimilarityDO.setCreator(String.valueOf(userId));
            smallImageIdList.add(targetSmallImageId);
            recallList.add(imgSimilarityDO);
          }
        }
      }
    }

    // 补充相似图片的 文章id 和 大图id
    List<SmallImageDO> smallImageDOList = smallImageService.queryByIds(smallImageIdList);
    Map<Long,SmallImageDO> smallImageDOMap = smallImageDOList.stream().collect(Collectors.toMap(SmallImageDO::getId, x -> x));
    for (ImgSimilarityDO imgSimilarityDO : recallList) {
      SmallImageDO smallImageDO = smallImageDOMap.get(imgSimilarityDO.getTargetSmallImageId());
      if (Objects.nonNull(smallImageDO)) {
        imgSimilarityDO.setTargetArticleId(smallImageDO.getArticleId());
        imgSimilarityDO.setTargetLargeImageId(smallImageDO.getLargeImageId());
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
    if (!CollectionUtils.isAnyEmpty(recallList)){
      Boolean flag = imgSimilarityService.batchInsert(recallList);
      if (!flag){
        log.error("批量写入相似图片对失败");
      }
    }
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

  private Map<String,List<Float>> getVector(String vectorPath){

    Map<String,List<Float>> vectorMap = Maps.newHashMap();

    CsvReadConfig config = CsvReadConfig.defaultConfig()
        .setFieldSeparator('\t');

    // 2. 读取 CSV 文件
    CsvReader reader = CsvUtil.getReader(config);
    CsvData data = reader.read(FileUtil.file(vectorPath));

    // 3. 遍历数据
    for (CsvRow row : data.getRows()) {
      String model = row.get(0);
      String vectorStr = row.get(1);
      List<Float> vectorArray = Arrays.stream(vectorStr.substring(1, vectorStr.length() - 1).split(","))
          .map(String::trim)
          .map(Float::parseFloat)
          .collect(Collectors.toList());
      vectorMap.put(model, vectorArray);
    }
    return  vectorMap;
  }


  public List<SmallImageMilvusDTO> processFile(String file) {

    // 1.pdf
    PdfParseResultDTO parseResult = pdfParseService.parsePdf(file);
    if (Objects.isNull(parseResult)) {
      parseResult = new PdfParseResultDTO();
      parseResult.setPublicationDate(String.valueOf(System.currentTimeMillis()));
    }

    // 2.创建文章
    File pdfFile = new File(file);
    ArticleDO articleDO = new ArticleDO();
    articleDO.setIsImage(0);
    articleDO.setIsSource(1);
    articleDO.setFileName(pdfFile.getName());
    articleDO.setFilePath(file);
    articleDO.setFileSize(pdfFile.length()/1024);
    articleDO.setFileType("pdf");
    pdfParseService.transArticleToPdf(articleDO, parseResult);
    articleDO.setCreator("1");
    articleService.create(articleDO);

    // 3.调py接口：切割大图小图 & 小图向量化
    List<ProcessImageRequest> request = Lists.newArrayList();
    ProcessImageRequest imageRequest = new ProcessImageRequest();
    imageRequest.setArticleId(articleDO.getId());
    imageRequest.setFilePath(articleDO.getFilePath());
    imageRequest.setFileType(articleDO.getFileType());
    if (SpringUtils.isLocal()){
      imageRequest.setLargePrefixPath(String.format(DB_LARGE_PATH, articleDO.getId()));
      imageRequest.setSmallPrefixPath(String.format(DB_SMALL_PATH, articleDO.getId()));
    }else {
      imageRequest.setLargePrefixPath(String.format(DB_LARGE_PATH_DEV, articleDO.getId()));
      imageRequest.setSmallPrefixPath(String.format(DB_SMALL_PATH_DEV, articleDO.getId()));
    }
    request.add(imageRequest);

    String response = HttpUtils.post(url,null, JSONObject.toJSONString(request));
    log.info(response);
    if (StringUtils.isBlank(response)){
      log.error("处理失败，无返回，{}",articleDO.getId()); // todo 写库
      return Lists.newArrayList();
    }
    JSONObject jsonObject = JSONObject.parseObject(response);
    if (!"0000".equals(jsonObject.getString("code")) ) {
      log.error("处理失败,taskId={}, result : {}", articleDO.getId(), jsonObject.getString("code"));
      return Lists.newArrayList();
    }

    String resultStr = jsonObject.getString("data");
    if (StringUtils.isBlank(resultStr)) {
      log.error("处理失败,taskId={}, result : {}", articleDO.getId(), jsonObject.getString("code"));
      return Lists.newArrayList();
    }

    // 4.将大图小图写入数据库
    List<SmallImageMilvusDTO> allSmallList = Lists.newArrayList();
    List<ProcessImageResponse> responseList = JSONObject.parseArray(resultStr, ProcessImageResponse.class);
    for (ProcessImageResponse processImageResponse : responseList) {
      Long articleId = processImageResponse.getArticleId();
      List<LargeImage> largeImageList = processImageResponse.getLargeImageList();
      for (LargeImage largeImage : largeImageList) {
        LargeImageDO largeImageDO = transLargeImageDO(largeImage,articleId);
        Integer number = largeImageService.insert(largeImageDO);
        if (Objects.isNull(number) || number <= 0) {
          log.error("写入失败");
          continue;
        }

        List<SmallImageDO> smallImageDOList = Lists.newArrayList();
        List<SmallImage> smallImageList = largeImage.getSmallImageList();
        for (SmallImage smallImage : smallImageList) {
          smallImageDOList.add(transSmallImageDO(smallImage,articleId, largeImageDO.getId()));
        }
        Boolean flag = smallImageService.batchSave(smallImageDOList);
        if (!flag) {
          log.error("aaa");
          return Lists.newArrayList();
        }
        for (SmallImageDO smallImageDO : smallImageDOList){
          SmallImageMilvusDTO smallImageMilvusDTO = new SmallImageMilvusDTO();
          BeanUtils.copyProperties(smallImageDO, smallImageMilvusDTO);
          smallImageMilvusDTO.setAuthor(articleDO.getAuthorName());
          smallImageMilvusDTO.setKeywords(articleDO.getArticleKeywords());
          smallImageMilvusDTO.setArticleDate(articleDO.getArticleDate());
          smallImageMilvusDTO.setInstitution(articleDO.getAuthorInstitution());
          smallImageMilvusDTO.setSpecialty(articleDO.getMedicalSpecialty());
          allSmallList.add(smallImageMilvusDTO);
        }
      }
    }

    // 向量入库
    for (SmallImageMilvusDTO smallImageMilvusDTO : allSmallList) {
      String vectorPath = smallImageMilvusDTO.getVectorPath();
      Map<String,List<Float>> vectorMap = getVector(vectorPath);
      for(String modelName : vectorMap.keySet()) {
        if (modelName.equals("Resnet50")){
          smallImageMilvusDTO.setResnet50Vectors(vectorMap.get(modelName));
        }
      }
    }
    return allSmallList;
  }

  private LargeImageDO transLargeImageDO(LargeImage largeImage, Long articleId){
    LargeImageDO largeImageDO = new LargeImageDO();
    largeImageDO.setIsProcessed(1);
    largeImageDO.setImagePath(largeImage.getPath());
    largeImageDO.setImageFormat("jpg");
    largeImageDO.setArticleId(articleId);
    largeImageDO.setCaption(largeImage.getCaption());
    largeImageDO.setPageNumber(largeImage.getPage_number());
    largeImageDO.setCaption(largeImage.getCaption());
    File tmpfile = new File(largeImage.getPath());
    largeImageDO.setImageFileName(tmpfile.getName());
    largeImageDO.setImageSize(tmpfile.length()/1024); //kb
    return largeImageDO;
  }

  private SmallImageDO transSmallImageDO(SmallImage smallImage, Long articleId, Long largeImageId){
    String path = smallImage.getPath();
    String name = path.substring(path.lastIndexOf("/")+1);
    log.info("path={},name={}", path, name);
    String imageUrl = path + "/" + name + ".jpg";
    String vectorPath = path + "/" + name + ".csv";
    SmallImageDO smallImageDO = new SmallImageDO();
    smallImageDO.setArticleId(articleId);
    smallImageDO.setLargeImageId(largeImageId);
    File imageFile = new File(imageUrl);
    smallImageDO.setImageSize(imageFile.length()/1024);
    smallImageDO.setImageType("small");
    smallImageDO.setImageName(name + ".jpg");
    smallImageDO.setCreator(String.valueOf(WebFrameworkUtils.getLoginUserId()));
    smallImageDO.setImagePath(imageUrl);
    smallImageDO.setVectorPath(vectorPath);
    smallImageDO.setCreator("1");
    return smallImageDO;
  }

}
