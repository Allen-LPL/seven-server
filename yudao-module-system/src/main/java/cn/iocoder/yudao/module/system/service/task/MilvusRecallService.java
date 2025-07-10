package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.api.task.common.ImageProcessService;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.enums.task.MilvusConstant;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.KeyValuePair;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.alias.AlterAliasParam;
import io.milvus.param.alias.CreateAliasParam;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DescribeCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.InsertParam.Field;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.response.SearchResultsWrapper.IDScore;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MilvusRecallService {

  @Resource
  private MilvusServiceClient imageMilvusClient;

  private static final String param = "{\"nprobe\":10}";

  public List<Map<String,Object>> recall(List<Float> vector,String model) {
    List<Map<String,Object>> result =  Lists.newArrayList();

    Stopwatch stopwatch = Stopwatch.createStarted();
    List<List<Float>> searchVectors = Arrays.asList(vector);

    SearchParam searchParam = SearchParam.newBuilder()
        .withCollectionName("resnet50_vectors")
        .withMetricType(MetricType.IP)
        .withOutFields(Lists.newArrayList(MilvusConstant.imageId))
        .withTopK(30)
        .withVectors(searchVectors)
        .withVectorFieldName(MilvusConstant.vectors)
        //.withExpr("score <= 11000")
        .withParams(param)
        .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
        .build();
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
}
