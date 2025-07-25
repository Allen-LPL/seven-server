package cn.iocoder.yudao.module.system.service.task;


import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.MilvusConstant;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.enums.task.TaskTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.VectorQueryTypeEnum;
import cn.iocoder.yudao.module.system.service.task.dto.VectorCalculateDTO;
import cn.iocoder.yudao.module.system.service.task.dto.VectorCalculateDTO.ScoreData;
import cn.iocoder.yudao.module.system.service.task.utils.CsvReadVectorUtils;
import cn.iocoder.yudao.module.system.service.task.utils.VectorCalculateUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class VectorQueryService {

  @Resource
  private TaskConfig taskConfig;

  @Resource
  private MilvusRecallService milvusRecallService;


  public List<ImgSimilarityDO> query(List<SmallImageDO> allSmallList, Long taskId, VectorQueryTypeEnum queryType,
      Integer taskType, String strategyConfig){
    List<ImgSimilarityDO> recallList = Lists.newArrayList();
    if (Objects.equals(TaskTypeEnum.FULL_DB_QUERY.getCode(), taskType)) {
      log.info("enter 全库查 ");
      recallList = fullDbQuery(allSmallList, taskId, queryType,strategyConfig);
      log.info("end 全库查 , size : {}", recallList.size());
    }else if (Objects.equals(TaskTypeEnum.INNER_QUERY.getCode(), taskType)){
      log.info("enter 篇内查 ");
      recallList = innerQuery(allSmallList, taskId, queryType);
      log.info("end 篇内查 , size : {}", recallList.size());
    }else if (Objects.equals(TaskTypeEnum.STRATEGY_QUERY.getCode(), taskType)){
      log.info("enter 策略查 ");
      recallList = strategyDbQuery(allSmallList, taskId, queryType, strategyConfig);
      log.info("end 策略查 , size : {}", recallList.size());
    }
    return recallList;
  }

  public List<ImgSimilarityDO> fullDbQuery(List<SmallImageDO> allSmallList, Long taskId, VectorQueryTypeEnum queryType,String strategyConfig){

    Long userId = 1L;
    if (Objects.nonNull(WebFrameworkUtils.getLoginUserId())){
      userId = WebFrameworkUtils.getLoginUserId();
    }

    List<ImgSimilarityDO> recallList = Lists.newArrayList();
    for (SmallImageDO smallImageDO : allSmallList) {
      String vectorPath = smallImageDO.getVectorPath().replace(FilePathConstant.local_prefix,taskConfig.getReplacePrefix());
      log.info("vectorPath = {}", vectorPath);
      Map<String,List<Double>> vectorMap = CsvReadVectorUtils.readVector(vectorPath);
      for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
        log.info("taskId={}, start vector recall: {}, model : {}", taskId, vectorPath, modelNameEnum.getCode());
        List<Float> floatList = vectorMap.get(modelNameEnum.getL2VectorName()).stream().map(Double::floatValue).collect(Collectors.toList());
        recallList.addAll(recall(floatList,taskId,queryType,userId,modelNameEnum,smallImageDO,strategyConfig));
        log.info("taskId={}, end vector recall: {}, model : {}", taskId, vectorPath, modelNameEnum.getCode());
      }
    }
    return recallList;
  }


  private List<ImgSimilarityDO> recall(List<Float> floatList, Long taskId, VectorQueryTypeEnum queryType,Long userId,ModelNameEnum modelNameEnum,
      SmallImageDO smallImageDO, String strategyConfig){
    List<ImgSimilarityDO> recallList = Lists.newArrayList();
    List<Map<String,Object>> resultList = milvusRecallService.recall(floatList, modelNameEnum.getCollectionName(), queryType, strategyConfig);
    log.info("end vector resultList: {}", JSONObject.toJSONString(resultList));
    for (Map<String,Object> imageIdScoreMap : resultList) {
      Double similarScore = MapUtils.getDoubleValue(imageIdScoreMap, MilvusConstant.SCORE,0.0f);
      if (similarScore < modelNameEnum.getScore()){
        continue;
      }
      ImgSimilarityDO imgSimilarityDO = new ImgSimilarityDO();
      imgSimilarityDO.setTaskId(taskId);
      imgSimilarityDO.setAlgorithmName(modelNameEnum.getCode());
      imgSimilarityDO.setSourceArticleId(smallImageDO.getArticleId());
      imgSimilarityDO.setSourceLargeImageId(smallImageDO.getLargeImageId());
      imgSimilarityDO.setSourceSmallImageId(smallImageDO.getId());
      imgSimilarityDO.setSimilarityScore(similarScore);
      Long targetSmallImageId = MapUtils.getLong(imageIdScoreMap, MilvusConstant.imageId,0L);
      imgSimilarityDO.setTargetSmallImageId(targetSmallImageId);
      imgSimilarityDO.setCreator(String.valueOf(userId));
      recallList.add(imgSimilarityDO);
    }
    return recallList;
  }

  public List<ImgSimilarityDO> strategyDbQuery(List<SmallImageDO> allSmallList, Long taskId, VectorQueryTypeEnum queryType,
      String strategyConfig){
    return fullDbQuery(allSmallList, taskId, queryType, strategyConfig);
  }

  public List<ImgSimilarityDO> innerQuery(List<SmallImageDO> allSmallList, Long taskId, VectorQueryTypeEnum queryType){
    List<ImgSimilarityDO> recallList = Lists.newArrayList();

    // 获取每个小图的向量
    List<VectorCalculateDTO> vectorCalculateDTOList =  Lists.newArrayList();
    for (SmallImageDO smallImageDO : allSmallList) {
      String vectorPath = smallImageDO.getVectorPath().replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix());
      Map<String,List<Double>> vectorMap = CsvReadVectorUtils.readVector(vectorPath);
      VectorCalculateDTO vectorCalculateDTO = new VectorCalculateDTO();
      vectorCalculateDTO.setSmallImageId(smallImageDO.getId());
      for (String modelName : vectorMap.keySet()) {
        if (ModelNameEnum.ResNet50.getL2VectorName().equals(modelName)) {
          vectorCalculateDTO.setResnet50(vectorMap.get(modelName));
        }else if (ModelNameEnum.CLIP.getL2VectorName().equals(modelName)) {
          vectorCalculateDTO.setClipVit(vectorMap.get(modelName));
        }else if (ModelNameEnum.DenseNet121.getL2VectorName().equals(modelName)) {
          vectorCalculateDTO.setDenseNet121(vectorMap.get(modelName));
        }else if (ModelNameEnum.SwinTransformer.getL2VectorName().equals(modelName)) {
          vectorCalculateDTO.setSwinTransformer(vectorMap.get(modelName));
        }else if (ModelNameEnum.DINOv2.getL2VectorName().equals(modelName)) {
          vectorCalculateDTO.setDinoV2(vectorMap.get(modelName));
        }
      }
      vectorCalculateDTO.setArticleId(smallImageDO.getArticleId());
      vectorCalculateDTO.setLargeImageId(smallImageDO.getLargeImageId());
      vectorCalculateDTOList.add(vectorCalculateDTO);
    }

    for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
      recallList.addAll(calScore(vectorCalculateDTOList,queryType,modelNameEnum,taskId));
    }

    return recallList;
  }

  private List<ImgSimilarityDO>  calScore(List<VectorCalculateDTO> vectorCalculateDTOList, VectorQueryTypeEnum queryType,
      ModelNameEnum modelNameEnum, Long taskId){
    List<ImgSimilarityDO> recallList = Lists.newArrayList();

    // 计算小图和其他小图的向量分
    Set<String> exits = Sets.newHashSet();
    for (VectorCalculateDTO v1 : vectorCalculateDTOList) {
      List<ScoreData> similarList = Lists.newArrayList();
      for (VectorCalculateDTO v2 : vectorCalculateDTOList) {
        if (Objects.equals(v1.getSmallImageId(), v2.getSmallImageId())){
          continue;
        }
        String v1AndV2 = v1.getSmallImageId() + "," + v2.getSmallImageId();
        String v2AndV1 = v2.getSmallImageId() + "," + v1.getSmallImageId();
        if (!exits.contains(v2AndV1) && !exits.contains(v1AndV2)) {
          double score = 0.0;
          if (Objects.equals(ModelNameEnum.ResNet50, modelNameEnum)) {
            score = VectorCalculateUtils.computeSimilarity(v1.getResnet50(),v2.getResnet50(),queryType);
          }else if (Objects.equals(ModelNameEnum.DenseNet121, modelNameEnum)) {
            score = VectorCalculateUtils.computeSimilarity(v1.getDenseNet121(),v2.getDenseNet121(),queryType);
          }
          else if (Objects.equals(ModelNameEnum.DINOv2, modelNameEnum)) { // todo
            score = VectorCalculateUtils.computeSimilarity(v1.getResnet50(),v2.getResnet50(),queryType);
          }
          else if (Objects.equals(ModelNameEnum.CLIP, modelNameEnum)) {
            score = VectorCalculateUtils.computeSimilarity(v1.getClipVit(),v2.getClipVit(),queryType);
          }else if (Objects.equals(ModelNameEnum.SwinTransformer, modelNameEnum)) {
            score = VectorCalculateUtils.computeSimilarity(v1.getSwinTransformer(),v2.getSwinTransformer(),queryType);
          }

          similarList.add(new ScoreData(v2.getSmallImageId(),score,modelNameEnum.getCode()));
          exits.add(v2AndV1);
          exits.add(v1AndV2);
        }
      }
      v1.setSimilarList(similarList);
    }

    // 补全返回信息&只取top的向量分
    for (VectorCalculateDTO vObj : vectorCalculateDTOList) {

      // 获取相似列表
      List<ScoreData> similarList = vObj.getSimilarList();
      if (CollectionUtils.isEmpty(similarList)){
        continue;
      }

      // 根据相似分排序且过滤低分
      List<ScoreData> sortedSimilarList = similarList.stream()
          .sorted(Comparator.comparingDouble(ScoreData::getScore).reversed())
          .filter(x -> x.getScore() >= modelNameEnum.getScore())
          .collect(Collectors.toList());

      // 组装相似列表
      for (ScoreData similar : sortedSimilarList) {
        recallList.add(getImgSimilarityDO(taskId, vObj, similar));
      }
    }
    return recallList;
  }

  private ImgSimilarityDO getImgSimilarityDO(Long taskId, VectorCalculateDTO vObj, ScoreData similar) {
    ImgSimilarityDO imgSimilarityDO = new ImgSimilarityDO();
    imgSimilarityDO.setTaskId(taskId);
    imgSimilarityDO.setSourceSmallImageId(vObj.getSmallImageId());
    imgSimilarityDO.setSourceArticleId(vObj.getArticleId());
    imgSimilarityDO.setSourceLargeImageId(vObj.getLargeImageId());
    imgSimilarityDO.setTargetSmallImageId(similar.getSmallImageId());
    imgSimilarityDO.setIsSimilar(Boolean.TRUE);
    imgSimilarityDO.setSimilarityScore(similar.getScore());
    imgSimilarityDO.setAlgorithmName(similar.getModelName());
    return imgSimilarityDO;
  }

}
