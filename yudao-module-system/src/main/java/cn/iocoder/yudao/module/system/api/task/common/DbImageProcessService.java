package cn.iocoder.yudao.module.system.api.task.common;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageRequest;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage.SmallImage;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.api.task.utils.ImageBeanTransUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
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


  private static final String local_prefix = "./task-file/";
  private static final String DB_LARGE_PATH = "%sdb/%s/largeImage/";
  private static final String DB_SMALL_PATH = "%sdb/%s/smallImage/";


  private static final String url = "http://172.20.76.8:8086/process_articles";

  @Value("${image.replace.prefix}")
  private String replacePrefix;

  @Resource
  private Executor taskExecutor;

  @Resource
  private MilvusOperateService milvusOperateService;

  public void processFileBatchAsync(List<String> filePathList, String fileType, String collectionName){
    CompletableFuture.runAsync(() -> {
      processFileBatch(filePathList, fileType, collectionName);
    }, taskExecutor);
  }

  public void processFileBatch(List<String> filePathList, String fileType, String collectionName){
    for (String filePath : filePathList) {
      List<SmallImageMilvusDTO> smallImageMilvusDTOList = processFileSingle(filePath, fileType);
      milvusOperateService.writeData(collectionName,smallImageMilvusDTOList);
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

    // 3.调py接口：切割大图小图 & 小图向量化
    List<ProcessImageRequest> request = getProcessImageRequests(articleDO);
    String response = HttpUtils.post(url,null, JSONObject.toJSONString(request));
    log.info(response);
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
        LargeImageDO largeImageDO = ImageBeanTransUtils.transLargeImageDO(largeImage,articleId,replacePrefix,local_prefix);
        Integer number = largeImageService.insert(largeImageDO);
        if (Objects.isNull(number) || number <= 0) {
          log.error("写入失败");
          continue;
        }

        List<SmallImageDO> smallImageDOList = Lists.newArrayList();
        List<SmallImage> smallImageList = largeImage.getSmallImageList();
        for (SmallImage smallImage : smallImageList) {
          smallImageDOList.add(ImageBeanTransUtils.transSmallImageDO(smallImage,articleId, largeImageDO.getId(),replacePrefix,local_prefix));
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
      Map<String,List<Double>> vectorMap = CsvReadVectorUtils.readVector(vectorPath);
      for(String modelName : vectorMap.keySet()) {
        if (modelName.equals(ModelNameEnum.ResNet50.getCode())){
          List<Float> floatList = vectorMap.get(modelName).stream().map(Double::floatValue).collect(Collectors.toList());
          smallImageMilvusDTO.setResnet50Vectors(floatList);
        }
      }
    }
    return allSmallList;
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
    imageRequest.setFilePath(articleDO.getFilePath());
    imageRequest.setFileType(articleDO.getFileType());
    imageRequest.setLargePrefixPath(String.format(DB_LARGE_PATH,replacePrefix, articleDO.getId()));
    imageRequest.setSmallPrefixPath(String.format(DB_SMALL_PATH,replacePrefix, articleDO.getId()));
    request.add(imageRequest);
    return request;
  }

}
