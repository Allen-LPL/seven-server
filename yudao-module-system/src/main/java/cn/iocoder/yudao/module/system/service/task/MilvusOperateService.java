package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.api.task.common.DbImageProcessService;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.MilvusConstant;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.service.task.utils.CsvReadVectorUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.KeyValuePair;
import io.milvus.grpc.MutationResult;
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
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.InsertParam.Field;
import io.milvus.param.dml.UpsertParam;
import io.milvus.param.highlevel.dml.DeleteIdsParam;
import io.milvus.param.highlevel.dml.response.DeleteResponse;
import io.milvus.param.index.CreateIndexParam;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MilvusOperateService {

  @Resource
  private MilvusServiceClient imageMilvusClient;

  @Resource
  private DbImageProcessService dbImageProcessService;

  @Resource
  private ArticleService articleService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private TaskConfig taskConfig;


  public void fullDump(ModelNameEnum modelNameEnum){

    // 1.获取新旧collection名字
    log.info("【1/7】start get Collection name");
    String oldName = getOldCollectionName(modelNameEnum.getCollectionName());
    String newName = modelNameEnum.getCollectionName()+"_"+System.currentTimeMillis();
    log.info("【1/7】end get Collection name , oldName : {}, newName : {}", oldName, newName);


    // 2.创建结合
    log.info("【2/7】start create collection, newName={}, dim={}", newName, modelNameEnum.getDim());
    boolean flag = createCollection(newName,modelNameEnum.getDim());
    if(!flag){
      log.error("newName : {} ,创建失败", newName);
    }
    log.info("【2/7】end create collection, newName={}, dim={}", newName, modelNameEnum.getDim());

    // 3.创建index
    log.info("【3/7】start create index, newName={}", newName);
    createVectorIndex(newName);
    createScalarIndex(newName);
    log.info("【3/7】end create index, newName={}", newName);

    // 4.写入数据
    log.info("【4/7】start write data, newName={}", newName);
    batchWriteDataFromDb(newName, modelNameEnum);
    log.info("【4/7】end write data, newName={}", newName);

    // 5.切换别名
    log.info("【5/7】start change alias, newName={}, oldName={}, alias={}", newName,oldName,modelNameEnum.getCollectionName());
    renameAliasToRealCollection(newName,oldName, modelNameEnum.getCollectionName());
    log.info("【5/7】end change alias, newName={}, oldName={}, alias={}", newName,oldName,modelNameEnum.getCollectionName());

    // 6.load new
    log.info("【6/7】start load new collection, newName={}", newName);
    loadCollection(newName);
    log.info("【6/7】end load new collection, newName={}, old doc count : {}, new doc count : {}", newName, collectionDocCount(oldName), collectionDocCount(newName));

    // 6.删除旧索引
    log.info("【7/7】start release and delete old collection, oldName={}", oldName);
    releaseCollection(oldName);
    collectionDelete(oldName);
    log.info("【7/7】end release and delete old collection, oldName={}", oldName);
  }

  public void writeSingleFile(String indexName, String filePath, String fileType){
    if (StringUtils.isBlank(filePath) || StringUtils.isBlank(fileType) || StringUtils.isBlank(indexName)) {
      return;
    }
    List<SmallImageMilvusDTO> smallImageMilvusDTOS =  dbImageProcessService.processFileSingle(filePath,fileType);
    List<SmallImageMilvusDTO> batchList = Lists.newArrayList();
    for (SmallImageMilvusDTO smallImageMilvusDTO : smallImageMilvusDTOS) {
      batchList.add(smallImageMilvusDTO);
      if (batchList.size() >= 10) {
        writeDataOneCollection(indexName, batchList);
        batchList.clear();
      }
    }
    if (!batchList.isEmpty()){
      writeDataOneCollection(indexName,batchList);
    }
  }

  public void batchWriteDataFromDb(String newName,ModelNameEnum modelNameEnum){
    Long maxId = articleService.sourceMaxId();
    Long minId = articleService.sourceMinId();
//    Long maxId = 377L;
//    Long minId = 355L;
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
          Map<String,List<Double>> vectorMap = CsvReadVectorUtils.readVector(smallImageDO.getVectorPath()
              .replace(FilePathConstant.local_prefix,taskConfig.getReplacePrefix()));
          List<Float> floatList = vectorMap.get(modelNameEnum.getL2VectorName()).stream().map(Double::floatValue)
              .collect(Collectors.toList());
          smallImageMilvusDTO.setVectors(floatList);
          smallImageMilvusDTO.setAuthor(articleDO.getAuthorName());
          smallImageMilvusDTO.setSpecialty(articleDO.getMedicalSpecialty());
          smallImageMilvusDTO.setArticleDate(articleDO.getArticleDate());
          smallImageMilvusDTO.setArticleId(articleDO.getId());
          smallImageMilvusDTO.setInstitution(articleDO.getAuthorInstitution());
          smallImageMilvusDTO.setKeywords(articleDO.getArticleKeywords());
          batchList.add(smallImageMilvusDTO);
        }
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(batchList)){
          writeDataOneCollection(newName,batchList);
        }
      }

      log.info("completeOfflineLabels min : {}, max :{}",minId,maxId);
      minId = articleDOList.get(articleDOList.size()-1).getId();
    }
  }


  public void  writeDataOneCollection(String newName, List<SmallImageMilvusDTO> smallImageMilvusDTOS){

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
        List<String> keywords = imageMilvusDTO.getKeywords().subList(0,Math.min(imageMilvusDTO.getKeywords().size(),6));
        keywords = keywords.stream().map(word -> word.substring(0,Math.min(999,word.length())).toLowerCase()).collect(Collectors.toList());
        keywordList.add(keywords);
      }
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getAuthor())){
        authorList.add(Lists.newArrayList());
      }else {
        List<String> authors = imageMilvusDTO.getAuthor().subList(0,Math.min(imageMilvusDTO.getAuthor().size(),6));
        authors = authors.stream().map(word -> word.substring(0,Math.min(999,word.length())).toLowerCase()).collect(Collectors.toList());
        authorList.add(authors);
      }
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getInstitution())){
        institutionList.add(Lists.newArrayList());
      } else {
        List<String> institutions = imageMilvusDTO.getInstitution().subList(0,Math.min(imageMilvusDTO.getInstitution().size(),6));
        institutions = institutions.stream().map(String::toLowerCase).collect(Collectors.toList());
        institutionList.add(institutions);
      }

      if (Objects.isNull(imageMilvusDTO.getArticleDate())){
        articleDateList.add(0L);
      }else {
        articleDateList.add(imageMilvusDTO.getArticleDate());
      }

      if (StringUtils.isNotBlank(imageMilvusDTO.getSpecialty())){
        specialtyList.add(imageMilvusDTO.getSpecialty().toLowerCase());
      }else {
        specialtyList.add("");
      }

      vectorList.add(imageMilvusDTO.getVectors()); //todo
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

  public void  writeDataAllCollection(List<SmallImageMilvusDTO> smallImageMilvusDTOS){

    for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
      smallImageMilvusDTOS.forEach(smallImageMilvusDTO -> {
        if (Objects.equals(ModelNameEnum.ResNet50,modelNameEnum)){
          smallImageMilvusDTO.setVectors(smallImageMilvusDTO.getResnet50());
        }else if (Objects.equals(ModelNameEnum.DINOv2,modelNameEnum)){
          smallImageMilvusDTO.setVectors(smallImageMilvusDTO.getDinoV2());
        }else if (Objects.equals(ModelNameEnum.DenseNet121,modelNameEnum)){
          smallImageMilvusDTO.setVectors(smallImageMilvusDTO.getDenseNet121());
        }else if (Objects.equals(ModelNameEnum.CLIP,modelNameEnum)){
          smallImageMilvusDTO.setVectors(smallImageMilvusDTO.getClipVit());
        }else if (Objects.equals(ModelNameEnum.SwinTransformer,modelNameEnum)){
          smallImageMilvusDTO.setVectors(smallImageMilvusDTO.getSwinTransformer());
        }
      });
      writeDataOneCollection(modelNameEnum.getCollectionName(), smallImageMilvusDTOS);
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
            .withMaxLength(1000)
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
            .withMaxLength(1000)
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
            .withMaxLength(1000)
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
        //.withIndexType(IndexType.IVF_FLAT)
        .withIndexType(IndexType.HNSW)
        .withMetricType(MetricType.COSINE)
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
    if (StringUtils.isBlank(collectionName)) {
      return true;
    }
    ReleaseCollectionParam releaseCollectionParam = ReleaseCollectionParam.newBuilder()
        .withCollectionName(collectionName)
        .build();
    R<RpcStatus> response = imageMilvusClient.releaseCollection(releaseCollectionParam);
    return response!= null && response.getStatus() == 0;
  }

  public boolean collectionDelete(String collectionName) {
    if (StringUtils.isBlank(collectionName)) {
      return true;
    }
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
    if (StringUtils.isBlank(collectionName)) {
      return 0;
    }
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

  public boolean  deleteByPrimaryId(List<Long> smallImageIds, String collectionName) {

    DeleteIdsParam deleteIdsParam = DeleteIdsParam.newBuilder().withCollectionName(collectionName)
        .withPrimaryIds(smallImageIds)
        .build();
    R<DeleteResponse> deleteResponseR = imageMilvusClient.delete(deleteIdsParam);
    // 完整的null检查
    if (deleteResponseR == null) {
      log.error("Milvus删除操作返回null响应, collection: {}, ids: {}", collectionName, smallImageIds);
      return false;
    }

    log.info("delete response status: {}, message: {}",
        deleteResponseR.getStatus(), deleteResponseR.getMessage());

    // 检查操作状态（Milvus通常状态码0表示成功）
    if (deleteResponseR.getStatus() != 0) {
      log.error("Milvus删除失败, collection: {}, ids: {}, status: {}, message: {}",
          collectionName, smallImageIds, deleteResponseR.getStatus(), deleteResponseR.getMessage());
      return false;
    }

    // 检查响应数据
    DeleteResponse data = deleteResponseR.getData();
    if (data == null) {
      log.warn("Milvus删除响应数据为null, 但状态码成功, collection: {}, ids: {}",
          collectionName, smallImageIds);
      return true; // 状态码成功，可能还是认为操作成功
    }

    log.info("成功删除数据, collection: {}, 删除ID数: {}",collectionName, smallImageIds.size());

    return true;
  }

  public void updateByPrimaryId(List<SmallImageMilvusDTO> smallImageMilvusDTOS) {

    List<Long> imageIdList = Lists.newArrayList();
    List<List<String>> keywordList = Lists.newArrayList();
    List<List<String>> authorList = Lists.newArrayList();
    List<List<String>> institutionList = Lists.newArrayList();
    List<Long> articleDateList = Lists.newArrayList();
    List<String> specialtyList = Lists.newArrayList();

    for (SmallImageMilvusDTO imageMilvusDTO : smallImageMilvusDTOS) {
      imageIdList.add(imageMilvusDTO.getId());
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getKeywords())){
        keywordList.add(Lists.newArrayList());
      }else {
        List<String> keywords = imageMilvusDTO.getKeywords().subList(0,Math.min(imageMilvusDTO.getKeywords().size(),6));
        keywords = keywords.stream().map(word -> word.substring(0,Math.min(999,word.length())).toLowerCase()).collect(Collectors.toList());
        keywordList.add(keywords);
      }
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getAuthor())){
        authorList.add(Lists.newArrayList());
      }else {
        List<String> authors = imageMilvusDTO.getAuthor().subList(0,Math.min(imageMilvusDTO.getAuthor().size(),6));
        authors = authors.stream().map(word -> word.substring(0,Math.min(999,word.length())).toLowerCase()).collect(Collectors.toList());
        authorList.add(authors);
      }
      if (CollectionUtils.isAnyEmpty(imageMilvusDTO.getInstitution())){
        institutionList.add(Lists.newArrayList());
      } else {
        List<String> institutions = imageMilvusDTO.getInstitution().subList(0,Math.min(imageMilvusDTO.getInstitution().size(),6));
        institutions = institutions.stream().map(String::toLowerCase).collect(Collectors.toList());
        institutionList.add(institutions);
      }

      if (Objects.isNull(imageMilvusDTO.getArticleDate())){
        articleDateList.add(0L);
      }else {
        articleDateList.add(imageMilvusDTO.getArticleDate());
      }

      if (StringUtils.isNotBlank(imageMilvusDTO.getSpecialty())){
        specialtyList.add(imageMilvusDTO.getSpecialty().toLowerCase());
      }else {
        specialtyList.add("");
      }
    }

    List<Field> fieldDataList = Lists.newArrayList();
    fieldDataList.add(new InsertParam.Field(MilvusConstant.imageId, imageIdList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.keywords, keywordList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.author, authorList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.institution, institutionList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.articleDate,  articleDateList));
    fieldDataList.add(new InsertParam.Field(MilvusConstant.specialty,  specialtyList));

    for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
      UpsertParam upsertParam = UpsertParam.newBuilder().withCollectionName(modelNameEnum.getCollectionName())
          .withFields(fieldDataList).build();
      R<MutationResult>  resultR = imageMilvusClient.upsert(upsertParam);
      log.info("update response : {}", JSONObject.toJSONString(resultR));
      if (resultR == null || resultR.getStatus() != 0) {
        log.error("update error : {}", JSONObject.toJSONString(resultR));
      }
    }
  }
}
