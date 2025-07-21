package cn.iocoder.yudao.module.system.service.task;


import cn.iocoder.yudao.module.system.api.task.dto.TaskStrategyConfig;
import cn.iocoder.yudao.module.system.enums.task.MilvusConstant;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.enums.task.VectorQueryTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.response.SearchResultsWrapper.IDScore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MilvusRecallService {

  @Resource
  private MilvusServiceClient imageMilvusClient;

  //private static final String param = "{\"nprobe\":10}";
  private static final String param = "{\"ef\":10}";

  public List<Map<String,Object>> recall(List<Float> vector,String model, VectorQueryTypeEnum queryType, String strategy) {
    List<Map<String,Object>> result =  Lists.newArrayList();

    // 获取metric type
    MetricType metricType = MetricType.COSINE;
    if (Objects.equals(VectorQueryTypeEnum.L2 , queryType)) {
      metricType = MetricType.L2;
    }else if (Objects.equals(VectorQueryTypeEnum.IP , queryType)) {
      metricType = MetricType.IP;
    }

    // 获取Collection名称
    String collectionName = getCollectionName(model);

    // 获取策略
    List<String>  exprList = Lists.newArrayList();
    if (StringUtils.isNotBlank(strategy)) {
      TaskStrategyConfig taskStrategyConfig = JSONObject.parseObject(strategy, TaskStrategyConfig.class);
      if (Objects.nonNull(taskStrategyConfig.getStartTime())){
        exprList.add( MilvusConstant.articleDate + " >= "+taskStrategyConfig.getStartTime());
      }else if (Objects.nonNull(taskStrategyConfig.getEndTime())){
        exprList.add(MilvusConstant.articleDate +" <= "+taskStrategyConfig.getEndTime());
      }else if (Objects.nonNull(taskStrategyConfig.getMedicalSpecialty())){
        exprList.add( MilvusConstant.specialty +" == '"+taskStrategyConfig.getMedicalSpecialty()+"'");
      }else if (CollectionUtils.isNotEmpty(taskStrategyConfig.getKeywordList())){
        String keywords = "[";
        keywords += StringUtils.join(taskStrategyConfig.getKeywordList(), ",");
        keywords += "]";
        exprList.add(MilvusConstant.keywords + "in " + keywords);
      }
    }
    String exp = "";
    if (CollectionUtils.isNotEmpty(exprList)) {
      exp = StringUtils.join(exprList, " AND ");
      log.info("milvusRecall exp: {}", exp);
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    List<List<Float>> searchVectors = Arrays.asList(vector);
    SearchParam searchParam = null;
    if (StringUtils.isNotBlank(exp)) {
      searchParam = SearchParam.newBuilder()
          .withCollectionName(collectionName)
          .withMetricType(metricType)
          .withOutFields(Lists.newArrayList(MilvusConstant.imageId))
          .withTopK(30)
          .withFloatVectors(searchVectors)
          .withVectorFieldName(MilvusConstant.vectors)
          .withExpr(exp)
          .withParams(param)
          .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
          .build();
    }else {
      searchParam = SearchParam.newBuilder()
          .withCollectionName(collectionName)
          .withMetricType(metricType)
          .withOutFields(Lists.newArrayList(MilvusConstant.imageId))
          .withTopK(30)
          .withFloatVectors(searchVectors)
          .withVectorFieldName(MilvusConstant.vectors)
          .withParams(param)
          .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
          .build();
    }
    R<SearchResults> respSearch = imageMilvusClient.search(searchParam);


    if (respSearch.getStatus() == 0){
      SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(respSearch.getData().getResults());
      List spuList = wrapperSearch.getFieldData(MilvusConstant.imageId, 0);
      List<IDScore> idScoreList = wrapperSearch.getIDScore(0);
      log.info("vector recall success , size is {}",spuList.size());

      for (int i=0; i<spuList.size(); i++){
        Map<String,Object> scoreMap = Maps.newHashMap();
        scoreMap.put(MilvusConstant.imageId,spuList.get(i));
        scoreMap.put("score",idScoreList.get(i).getScore());
        result.add(scoreMap);
      }
    }
    log.info("milvus recall use {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  private static String getCollectionName(String model) {
    String collectionName = "resnet50_vectors";
    if (ModelNameEnum.ResNet50.getL2VectorName().equals(model)) {
      collectionName = ModelNameEnum.ResNet50.getCollectionName();
    }else if (ModelNameEnum.DINOv2.getL2VectorName().equals(model)) {
      collectionName = ModelNameEnum.DINOv2.getCollectionName();
    }else if (ModelNameEnum.CLIP.getL2VectorName().equals(model)) {
      collectionName = ModelNameEnum.CLIP.getCollectionName();
    }else if (ModelNameEnum.SwinTransformer.getL2VectorName().equals(model)) {
      collectionName = ModelNameEnum.SwinTransformer.getCollectionName();
    }else if (ModelNameEnum.DenseNet121.getL2VectorName().equals(model)) {
      collectionName = ModelNameEnum.DenseNet121.getCollectionName();
    }
    return collectionName;
  }

}
