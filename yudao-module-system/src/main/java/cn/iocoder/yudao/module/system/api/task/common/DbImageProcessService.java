package cn.iocoder.yudao.module.system.api.task.common;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageRequest;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage.SmallImage;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.api.task.utils.ImageBeanTransUtils;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.FileTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import cn.iocoder.yudao.module.system.service.task.utils.CsvReadVectorUtils;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.PdfParseService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import cn.iocoder.yudao.module.system.service.task.VectorQueryService;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import cn.iocoder.yudao.module.system.util.FileWriterUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DbImageProcessService {

  @Resource
  private ArticleService articleService;

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private PdfParseService pdfParseService;

  @Resource
  private TaskConfig taskConfig;

  @Resource
  private Executor taskExecutor;

  @Resource
  private MilvusOperateService milvusOperateService;

  public void processFileBatchAsync(List<String> filePathList, String fileType){
    CompletableFuture.runAsync(() -> {
      processFileBatch(filePathList, fileType);
    }, taskExecutor);
  }

  public void processFileBatch(List<String> filePathList, String fileType){
    for (String filePath : filePathList) {
      List<SmallImageMilvusDTO> smallImageMilvusDTOList = processFileSingle(filePath, fileType);
      milvusOperateService.writeDataAllCollection(smallImageMilvusDTOList);
    }
  }

  public void batchHandleFileParentDirectory(String filePath, String fileType){
    File root = new File(filePath);
    File[] files = root.listFiles();
    if (files == null) return;

    List<String> fileList = Lists.newArrayList();
    for (File file : files) {
      if (file.isDirectory()) {
        continue;
      }
      if (fileType.equals("pdf") && file.getName().endsWith(".pdf")) {
        fileList.add(file.getAbsolutePath());
      }else if (fileType.equals("image") && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
        fileList.add(file.getAbsolutePath());
      }
    }

    if (CollectionUtils.isAnyEmpty(fileList)){
      log.warn("file Invalid , path {}",filePath);
      return;
    }

    for (String file : fileList) {
      processFileSingle(file,fileType);
    }
  }

  public void batchHandleFileList(List<String> filePathList, String fileType){
    List<String> fileList = Lists.newArrayList();
    for (String filePath : filePathList) {
      File file = new File(filePath);
      if (file.isDirectory()) {
        continue;
      }
      if (fileType.equals("pdf") && file.getName().endsWith(".pdf")) {
        fileList.add(file.getAbsolutePath());
      }else if (fileType.equals("image") && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
        fileList.add(file.getAbsolutePath());
      }
    }

    if (CollectionUtils.isAnyEmpty(fileList)){
      log.warn("file Invalid ");
      return;
    }

    for (String file : fileList) {
      processFileSingle(file,fileType);
    }
  }

  public List<SmallImageMilvusDTO> processFileSingle(String filePath, String fileType) {

    // 1.pdf
    ArticleDO articleDO = new ArticleDO();
    if (FileTypeEnum.PDF.getCode().equals(fileType)){
      PdfParseResultDTO parseResult = pdfParseService.parsePdf(filePath);
      if (Objects.isNull(parseResult)) {
        parseResult = new PdfParseResultDTO();
        parseResult.setPublicationDate(String.valueOf(System.currentTimeMillis()));
      }
      pdfParseService.transArticleToPdf(articleDO, parseResult);
    }

    // 2.创建文章
    File pdfFile = new File(filePath);
    articleDO.setIsImage(0);
    articleDO.setIsSource(1);
    articleDO.setFileName(pdfFile.getName());
    articleDO.setFilePath(filePath);
    articleDO.setFileSize(pdfFile.length()/1024);
    articleDO.setFileType(fileType);
    if (Objects.nonNull(WebFrameworkUtils.getLoginUserId())){
      articleDO.setCreator(String.valueOf(WebFrameworkUtils.getLoginUserId()));
    }else {
      articleDO.setCreator("1");
    }
    articleService.create(articleDO);

    // 0.上传文件
    String targetPath = String.format(FilePathConstant.DB_PATH, taskConfig.getReplacePrefix(), articleDO.getId())+pdfFile.getName();
    FileWriterUtils.copyFile(filePath, targetPath);
    articleDO.setFilePath(targetPath);
    ArticleDO updateArticle = new ArticleDO();
    updateArticle.setId(articleDO.getId());
    updateArticle.setFilePath(targetPath.replace(taskConfig.getReplacePrefix(), FilePathConstant.local_prefix));
    articleService.update(updateArticle);

    // 3.调py接口：切割大图小图 & 小图向量化
    List<ProcessImageRequest> request = getProcessImageRequests(articleDO);
    String response = HttpUtils.post(taskConfig.getProcessImageUrl(),null, JSONObject.toJSONString(request));
    log.info("cut image request : {}, response : {}", JSONObject.toJSONString(request), response);
    Optional<String> resultStr = getImageCutResultStr(response,articleDO.getId());
    if (!resultStr.isPresent() || StringUtils.isBlank(resultStr.get())){
      return Lists.newArrayList();
    }

    // 4.将大图小图写入数据库
    List<SmallImageMilvusDTO> allSmallList = Lists.newArrayList();
    List<ProcessImageResponse> responseList = JSONObject.parseArray(resultStr.get(), ProcessImageResponse.class);
    for (ProcessImageResponse processImageResponse : responseList) {
      Long articleId = processImageResponse.getArticleId();
      List<LargeImage> largeImageList = processImageResponse.getLargeImageList();
      for (LargeImage largeImage : largeImageList) {
        LargeImageDO largeImageDO = ImageBeanTransUtils.transLargeImageDO(largeImage,articleId, taskConfig.getReplacePrefix(),
            FilePathConstant.local_prefix);
        Integer number = largeImageService.insert(largeImageDO);
        if (Objects.isNull(number) || number <= 0) {
          log.error("写入失败");
          continue;
        }

        List<SmallImageDO> smallImageDOList = Lists.newArrayList();
        List<SmallImage> smallImageList = largeImage.getSmallImageList();
        for (SmallImage smallImage : smallImageList) {
          smallImageDOList.add(ImageBeanTransUtils.transSmallImageDO(smallImage,articleId, largeImageDO.getId(), taskConfig.getReplacePrefix(),
              FilePathConstant.local_prefix));
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
      Map<String,List<Double>> vectorMap = CsvReadVectorUtils.readVector(vectorPath.replace(FilePathConstant.local_prefix,
          taskConfig.getReplacePrefix()));
      for(String modelName : vectorMap.keySet()) {
        if (modelName.equals(ModelNameEnum.ResNet50.getL2VectorName())){
          List<Float> floatList = vectorMap.get(modelName).stream().map(Double::floatValue).collect(Collectors.toList());
          smallImageMilvusDTO.setResnet50(floatList);
        }else if (modelName.equals(ModelNameEnum.DINOv2.getL2VectorName())){
          List<Float> floatList = vectorMap.get(modelName).stream().map(Double::floatValue).collect(Collectors.toList());
          smallImageMilvusDTO.setDinoV2(floatList);
        }else if (modelName.equals(ModelNameEnum.DenseNet121.getL2VectorName())){
          List<Float> floatList = vectorMap.get(modelName).stream().map(Double::floatValue).collect(Collectors.toList());
          smallImageMilvusDTO.setDenseNet121(floatList);
        }else if (modelName.equals(ModelNameEnum.CLIP.getL2VectorName())){
          List<Float> floatList = vectorMap.get(modelName).stream().map(Double::floatValue).collect(Collectors.toList());
          smallImageMilvusDTO.setClipVit(floatList);
        }else if (modelName.equals(ModelNameEnum.SwinTransformer.getL2VectorName())){
          List<Float> floatList = vectorMap.get(modelName).stream().map(Double::floatValue).collect(Collectors.toList());
          smallImageMilvusDTO.setSwinTransformer(floatList);
        }
      }
    }
    return allSmallList;
  }

  public void batchRepeatHandleImage(List<Long> articleIdList){
    for (Long articleId : articleIdList) {
      repeatProcessFileSingle(articleId);
    }
  }

  public void repeatProcessFileSingle(Long articleId) {
    ArticleDO articleDO = articleService.batchQueryById(articleId);
    if(Objects.isNull(articleDO)){
      return;
    }

    // 3.调py接口：切割大图小图 & 小图向量化
    try {
      List<ProcessImageRequest> request = getProcessImageRequests(articleDO);
      String response = HttpUtils.post(taskConfig.getProcessImageUrl(),null, JSONObject.toJSONString(request));
      log.info(response);
      Optional<String> resultStr = getImageCutResultStr(response,articleDO.getId());
      log.info("resultStr : {}", resultStr);
    }catch (Exception e){
      log.error("repeat process file single error.", e);
    }
  }

  private Optional<String> getImageCutResultStr(String response, Long articleId){
    if (StringUtils.isBlank(response)){
      log.error("处理失败，无返回，{}",articleId); // todo 写库
      return Optional.empty();
    }
    JSONObject jsonObject = JSONObject.parseObject(response);
    if (!"0000".equals(jsonObject.getString("code")) ) {
      log.error("处理失败,taskId={}, result : {}", articleId, jsonObject.getString("code"));
      return Optional.empty();
    }

    String resultStr = jsonObject.getString("data");
    if (StringUtils.isBlank(resultStr)) {
      log.error("处理失败,taskId={}, result : {}", articleId, jsonObject.getString("code"));
      return Optional.empty();
    }

    return Optional.of(resultStr);
  }

  private List<ProcessImageRequest> getProcessImageRequests(ArticleDO articleDO) {
    List<ProcessImageRequest> request = Lists.newArrayList();
    ProcessImageRequest imageRequest = new ProcessImageRequest();
    imageRequest.setArticleId(articleDO.getId());
    imageRequest.setFilePath(articleDO.getFilePath().replace(FilePathConstant.local_prefix,taskConfig.getReplacePrefix()));
    imageRequest.setFileType(articleDO.getFileType());
    imageRequest.setLargePrefixPath(String.format(FilePathConstant.DB_LARGE_PATH, taskConfig.getReplacePrefix(), articleDO.getId()));
    imageRequest.setSmallPrefixPath(String.format(FilePathConstant.DB_SMALL_PATH, taskConfig.getReplacePrefix(), articleDO.getId()));
    request.add(imageRequest);
    return request;
  }


  public void addArticleByFilePath(String filePath, String fileType) {
    File root = new File(filePath);
    File[] files = root.listFiles();
    if (files == null) return;

    List<String> fileList = Lists.newArrayList();
    for (File file : files) {
      if (file.isDirectory()) {
        continue;
      }
      if (fileType.equals("pdf") && file.getName().endsWith(".pdf")) {
        fileList.add(file.getAbsolutePath());
      }else if (fileType.equals("image") && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
        fileList.add(file.getAbsolutePath());
      }
    }

    if (CollectionUtils.isAnyEmpty(fileList)){
      log.warn("file Invalid , path {}",filePath);
      return;
    }

    processFileBatch(fileList,fileType);
  }

}
