package cn.iocoder.yudao.module.system.service.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.api.task.common.ImageProcessService;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.MilvusConstant;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeAliasResponse;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.KeyValuePair;
import io.milvus.grpc.ListAliasesResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.alias.AlterAliasParam;
import io.milvus.param.alias.CreateAliasParam;
import io.milvus.param.alias.ListAliasesParam;
import io.milvus.param.bulkinsert.BulkInsertParam;
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
import io.milvus.param.index.CreateIndexParam;
import io.milvus.v2.service.utility.request.DescribeAliasReq;
import io.milvus.v2.service.utility.response.DescribeAliasResp;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

@Service
@Slf4j
public class MilvusOperateService {

  @Resource
  private MilvusServiceClient imageMilvusClient;

  @Resource
  private ImageProcessService imageProcessService;

  @Resource
  private ArticleService articleService;

  @Resource
  private SmallImageService smallImageService;

  public void fullDump(String alias,Integer dimension){
    // 1.获取新旧collection名字
    String oldName = getOldCollectionName(alias);
    String newName = alias+"_"+System.currentTimeMillis();
    log.info("oldName : {}, newName : {}", oldName, newName);

    // 2.创建结合
    boolean flag = createCollection(newName,dimension);
    if(!flag){
      log.error("newName : {} ,创建失败", newName);
    }

    // 3.创建index
    createVectorIndex(newName);
    createScalarIndex(newName);

    // 4.写入数据
    //batchWriteData(newName);
    batchWriteDataFromDb(newName);

    // 5.切换别名
    renameAliasToRealCollection(newName,oldName, alias);
    loadCollection(alias);
    log.info("old doc count : {}, new doc count : {}",collectionDocCount(oldName), collectionDocCount(newName));

    // 6.删除旧索引
    releaseCollection(oldName);
    collectionDelete(oldName);
  }

  public void batchWriteData(String newName){
    String path = "/Users/fangliu/Documents/pdf";
    File root = new File(path);
    File[] files = root.listFiles();
    if (files == null) return;

    List<String> fileList = Lists.newArrayList();
    for (File file : files) {
      if (file.isDirectory()) {
        continue;
      }
      if (file.getName().endsWith(".pdf")) {
        fileList.add(file.getAbsolutePath());
      }
    }

    for (String file : fileList) {
      List<SmallImageMilvusDTO> smallImageMilvusDTOS =  imageProcessService.processFile(file);
      List<SmallImageMilvusDTO> batchList = Lists.newArrayList();
      for (SmallImageMilvusDTO smallImageMilvusDTO : smallImageMilvusDTOS) {
        batchList.add(smallImageMilvusDTO);
        if (batchList.size() >= 10) {
          writeData(newName, batchList);
          batchList.clear();
        }
      }
      if (!batchList.isEmpty()){
        writeData(newName,batchList);
      }
    }
  }

  public void batchWriteDataFromDb(String newName){
    //Long maxId = articleService.maxId();
    //Long minId = articleService.minId();
    Long maxId = 311L;
    Long minId = 277L;
    int batch = 10;
    while (true){
      List<ArticleDO> articleDOList = articleService.queryByIdsBatch(minId,maxId,batch);
      if (org.apache.commons.collections4.CollectionUtils.isEmpty(articleDOList) || minId >= maxId){
        break;
      }

      for (ArticleDO articleDO : articleDOList) {
        List<SmallImageMilvusDTO> batchList = Lists.newArrayList();
        List<SmallImageDO> smallImageDOList = smallImageService.queryByArticleId(articleDO.getId());
        for (SmallImageDO smallImageDO : smallImageDOList) {
          SmallImageMilvusDTO smallImageMilvusDTO = new SmallImageMilvusDTO();
          smallImageMilvusDTO.setId(smallImageDO.getId());
          Map<String,List<Float>> vectorMap = getVector(smallImageDO.getVectorPath());
          smallImageMilvusDTO.setResnet50Vectors(vectorMap.get("Resnet50"));
          smallImageMilvusDTO.setAuthor(articleDO.getAuthorName());
          smallImageMilvusDTO.setSpecialty(articleDO.getMedicalSpecialty());
          smallImageMilvusDTO.setArticleDate(articleDO.getArticleDate());
          smallImageMilvusDTO.setArticleId(articleDO.getId());
          smallImageMilvusDTO.setInstitution(articleDO.getAuthorInstitution());
          smallImageMilvusDTO.setKeywords(articleDO.getArticleKeywords());
          batchList.add(smallImageMilvusDTO);
        }
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(batchList)){
          writeData(newName,batchList);
        }
      }

      log.info("completeOfflineLabels min : {}, max :{}",minId,maxId);
      minId = articleDOList.get(articleDOList.size()-1).getId();
    }
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

  public void  writeData(String newName, List<SmallImageMilvusDTO> smallImageMilvusDTOS){

    List<Long> imageIdList = Lists.newArrayList();
    List<List<String>> keywordList = Lists.newArrayList();
    List<List<String>> authorList = Lists.newArrayList();
    List<List<String>> institutionList = Lists.newArrayList();
    List<Long> articleDateList = Lists.newArrayList();
    List<String> specialtyList = Lists.newArrayList();
    List<List<Float>> vectorList = Lists.newArrayList();

    for (SmallImageMilvusDTO imageMilvusDTO : smallImageMilvusDTOS) {
      imageIdList.add(imageMilvusDTO.getId());
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getKeywords())){
        keywordList.add(Lists.newArrayList());
      }else {
        keywordList.add(imageMilvusDTO.getKeywords().subList(0,Math.min(imageMilvusDTO.getKeywords().size(),5)));
      }
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getAuthor())){
        authorList.add(Lists.newArrayList());
      }else {
        authorList.add(imageMilvusDTO.getAuthor().subList(0,Math.min(imageMilvusDTO.getAuthor().size(),5)));
      }
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getInstitution())){
        institutionList.add(Lists.newArrayList());
      } else {
        institutionList.add(imageMilvusDTO.getInstitution().subList(0,Math.min(imageMilvusDTO.getInstitution().size(),5)));
      }

      if (Objects.isNull(imageMilvusDTO.getArticleDate())){
        articleDateList.add(0L);
      }else {
        articleDateList.add(imageMilvusDTO.getArticleDate());
      }

      if (StringUtils.isNotBlank(imageMilvusDTO.getSpecialty())){
        specialtyList.add(imageMilvusDTO.getSpecialty());
      }else {
        specialtyList.add("");
      }
      vectorList.add(imageMilvusDTO.getResnet50Vectors()); //todo
    }

    List<Field> fieldDataList = Lists.newArrayList();
    fieldDataList.add(new InsertParam.Field(MilvusConstant.imageId, imageIdList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.keywords, keywordList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.author, authorList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.institution, institutionList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.articleDate,  articleDateList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.specialty,  specialtyList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.vectors,  vectorList));

    // 创建插入参数
    InsertParam insertParam = InsertParam.newBuilder()
        .withCollectionName(newName)
        .withFields(fieldDataList)
        .build();
    // 执行插入操作
    R<MutationResult> insert = imageMilvusClient.insert(insertParam);
    if (insert != null && insert.getStatus() == 0) {
      log.info("insert milvus collection success: {}",imageIdList.size());
    } else {
      log.error("insert milvus collection failed: {}",imageIdList.size());
    }
  }

  public boolean createCollection(String collectionName, Integer dimension){
    CollectionSchemaParam schemaParam = CollectionSchemaParam.newBuilder()
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.imageId)
            .withDataType(DataType.Int64)
            .withPrimaryKey(true)
            .withAutoID(false)
            .build())
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.keywords)
            .withDataType(DataType.Array)
            .withElementType(DataType.VarChar)
            .withMaxLength(50)
            .withMaxCapacity(15)
            .build())
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.author)
            .withDataType(DataType.Array)
            .withElementType(DataType.VarChar)
            .withMaxLength(100)
            .withMaxCapacity(6)
            .build())
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.institution)
            .withDataType(DataType.Array)
            .withElementType(DataType.VarChar)
            .withMaxLength(100)
            .withMaxCapacity(6)
            .build())
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.articleDate)
            .withDataType(DataType.Int64)
            .withMaxCapacity(64)
            .build())
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.specialty)
            .withDataType(DataType.VarChar)
            .withMaxLength(100)
            .build())
        .addFieldType(FieldType.newBuilder()
            .withName(MilvusConstant.vectors)
            .withDataType(DataType.FloatVector)
            .withDimension(dimension)  // 设置向量的维度
            .build())
        .build();
    CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
        .withCollectionName(collectionName)
        .withDescription("Collection to store image vectors")
        .withShardsNum(8)
        .withSchema(schemaParam)
        .build();
    R<RpcStatus> collection = imageMilvusClient.createCollection(createCollectionParam);
    return collection!=null && collection.getStatus() == 0;
  }

  public boolean createVectorIndex(String collectionName) {
    if (!collectionExist(collectionName)){
      log.info("向量数据不存在，无法创建索引:{}" , collectionName);
      return false ;
    }
    CreateIndexParam vectorIndexCreateParam = CreateIndexParam.newBuilder()
        .withCollectionName(collectionName)
        .withFieldName(MilvusConstant.vectors)
        .withIndexType(IndexType.IVF_FLAT)
        .withMetricType(MetricType.IP)
        .withExtraParam(MilvusConstant.params)
        .withSyncMode(Boolean.FALSE)
        .withIndexName(MilvusConstant.idx_image_vector)
        .build();
    R<RpcStatus> response = imageMilvusClient.createIndex(vectorIndexCreateParam);
    log.info("index-create : 创建向量索引 , 集合名称:{} ,是否成功:{} ", collectionName ,response.getStatus() == 0);
    return response.getStatus() == 0;
  }

  private void createScalarIndex(String collectionName){
    CreateIndexParam itemCategoryLevel1Index = CreateIndexParam.newBuilder()
        .withCollectionName(collectionName)
        .withFieldName(MilvusConstant.keywords)
        .withIndexName(MilvusConstant.idx_keywords)
        .withSyncMode(Boolean.FALSE)
        .build();
    R<RpcStatus> categoryLevel1IndexResp = imageMilvusClient.createIndex(itemCategoryLevel1Index);
    log.info("index-create : 创建一级类目标量索引, 集合名称:{} , 是否成功:{} ", collectionName ,categoryLevel1IndexResp.getStatus() == 0);


    CreateIndexParam itemCategoryLevel2Index = CreateIndexParam.newBuilder()
        .withCollectionName(collectionName)
        .withFieldName(MilvusConstant.author)
        .withIndexName(MilvusConstant.idx_author)
        .withSyncMode(Boolean.FALSE)
        .build();
    R<RpcStatus> categoryLevel2IndexResp = imageMilvusClient.createIndex(itemCategoryLevel2Index);
    log.info("index-create : 创建二级类目标量索引, 集合名称:{} , 是否成功:{} ", collectionName ,categoryLevel2IndexResp.getStatus() == 0);

    CreateIndexParam itemCountryCodeArrIndex = CreateIndexParam.newBuilder()
        .withCollectionName(collectionName)
        .withFieldName(MilvusConstant.institution)
        .withIndexName(MilvusConstant.idx_institution)
        .withSyncMode(Boolean.FALSE)
        .build();

    R<RpcStatus> countryCodeArrResp = imageMilvusClient.createIndex(itemCountryCodeArrIndex);
    log.info("index-create : 创建可售国家标量索引, 集合名称:{} , 是否成功:{} ", collectionName ,countryCodeArrResp.getStatus() == 0);

    CreateIndexParam itemTagsIndex = CreateIndexParam.newBuilder()
        .withCollectionName(collectionName)
        .withFieldName(MilvusConstant.articleDate)
        .withIndexName(MilvusConstant.idx_articleDate)
        .withSyncMode(Boolean.FALSE)
        .build();
    R<RpcStatus> itemTagsIndexResp = imageMilvusClient.createIndex(itemTagsIndex);
    log.info("index-create : 创建商品tags标量索引, 集合名称:{} , 是否成功:{} ", collectionName ,itemTagsIndexResp.getStatus() == 0);

    CreateIndexParam specialtyIndex = CreateIndexParam.newBuilder()
        .withCollectionName(collectionName)
        .withFieldName(MilvusConstant.specialty)
        .withIndexName(MilvusConstant.idx_specialty)
        .withSyncMode(Boolean.FALSE)
        .build();
    R<RpcStatus> specialtyIndexResp = imageMilvusClient.createIndex(specialtyIndex);
    log.info("index-create : 创建商品tags标量索引, 集合名称:{} , 是否成功:{} ", collectionName ,specialtyIndexResp.getStatus() == 0);
  }


  public boolean renameAliasToRealCollection(String collectionName,String oldName, String aliasName) {
    if(StringUtils.isBlank(oldName)){
      CreateAliasParam aliasCreate = CreateAliasParam.newBuilder()
          .withAlias(aliasName)
          .withCollectionName(collectionName)
          .build();
      R<RpcStatus> createResult = imageMilvusClient.createAlias(aliasCreate);
      return createResult!=null && createResult.getStatus() == 0;
    }else{
      AlterAliasParam aliasParam = AlterAliasParam.newBuilder()
          .withCollectionName(collectionName)
          .withAlias(aliasName)
          .build();
      R<RpcStatus> resp = imageMilvusClient.alterAlias(aliasParam);
      return resp != null && resp.getStatus() == 0;
    }
  }

  public String getOldCollectionName(String alias){
    R<DescribeCollectionResponse> responseR = imageMilvusClient.describeCollection(DescribeCollectionParam
        .newBuilder().withCollectionName(alias).build());
    if (responseR != null && responseR.getStatus() == 0) {
      DescribeCollectionResponse response = responseR.getData();
      return response.getSchema().getName();
    }
    return null;
  }

  public boolean collectionExist(String collectionName) {
    HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
        .withCollectionName(collectionName)
        .build();
    R<Boolean> hasCollectionResponse = imageMilvusClient.hasCollection(hasCollectionParam);
    return BooleanUtils.isTrue(hasCollectionResponse.getData());
  }

  public boolean loadCollection(String collectionName) {
    LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
        .withCollectionName(collectionName)
        .build();
    R<RpcStatus> response = imageMilvusClient.loadCollection(loadCollectionParam);
    return response!= null && response.getStatus() == 0;
  }

  public boolean releaseCollection(String collectionName) {
    ReleaseCollectionParam releaseCollectionParam = ReleaseCollectionParam.newBuilder()
        .withCollectionName(collectionName)
        .build();
    R<RpcStatus> response = imageMilvusClient.releaseCollection(releaseCollectionParam);
    return response!= null && response.getStatus() == 0;
  }

  public boolean collectionDelete(String collectionName) {
    if(!collectionExist(collectionName)){
      log.info("集合不存在，无法删除:{}" , collectionName);
      return false;
    }
    DropCollectionParam drop = DropCollectionParam.newBuilder()
        .withCollectionName(collectionName)
        .build();
    R<RpcStatus> rpcStatusR = imageMilvusClient.dropCollection(drop);
    return rpcStatusR!=null && rpcStatusR.getStatus() == 0;
  }

  public int collectionDocCount(String collectionName) {
    GetCollectionStatisticsParam statParam = GetCollectionStatisticsParam.newBuilder()
        .withCollectionName(collectionName)
        .build();
    R<GetCollectionStatisticsResponse> statResp = imageMilvusClient.getCollectionStatistics(statParam);
    if(statResp!=null && statResp.getStatus() == 0){
      KeyValuePair stats = statResp.getData().getStats(0);
      return Integer.parseInt(stats.getValue());
    }
    return 0;
  }
}
