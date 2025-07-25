package cn.iocoder.yudao;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.task.common.TaskImageProcessService;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.enums.task.VectorQueryTypeEnum;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import cn.iocoder.yudao.module.system.service.task.MilvusRecallService;
import cn.iocoder.yudao.module.system.service.task.utils.CsvReadVectorUtils;
import cn.iocoder.yudao.server.YudaoServerApplication;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest(classes = YudaoServerApplication.class)
public class TestMilvus {

  @Resource
  private MilvusOperateService milvusOperateService;

  @Resource
  private TaskImageProcessService imageProcessService;

  @Resource
  private MilvusRecallService milvusRecallService;

  @Resource
  private TaskConfig taskConfig;

  @Test
  public void fullDump() {
    TenantContextHolder.setTenantId(1L);
    milvusOperateService.fullDump(ModelNameEnum.ResNet50);
  }

  @Test
  public void testData(){
    TenantContextHolder.setTenantId(1L);
    String collectionName = "resnet50_vectors_1753093046455";
    milvusOperateService.batchWriteDataFromDb(collectionName, ModelNameEnum.ResNet50);
  }

  @Test
  public void testWriteVectors(){
    TenantContextHolder.setTenantId(1L);
    String collectionName = "resnet50_vectors_1751209930493";
    milvusOperateService.batchWriteDataFromDb(collectionName,ModelNameEnum.ResNet50);
  }

  @Test
  public void testDeleteCollection(){
    String collectionName = "resnet50_vectors_1752161613282";
    milvusOperateService.collectionDelete(collectionName);
  }

  @Test
  public void testManagerAlias(){
    String oldName = "resnet50_vectors_1752161613282";
    String newName = "resnet50_vectors_1752677026883";
    String alias = "resnet50_vectors";
    milvusOperateService.renameAliasToRealCollection(newName, oldName, alias);
  }

  @Test
  public void getMilvusCount(){
    String alias = "resnet50_vectors_1753093046455";
    int count = milvusOperateService.collectionDocCount(alias);
    log.info("[getMilvusCount][{}],count : {}", alias, count);
    milvusOperateService.loadCollection(alias);
  }

  @Test
  public void testMilvusRecall(){
    TenantContextHolder.setTenantId(1L);
    imageProcessService.process(49L);
  }

  @Test
  public void testMilvusName(){
    String alias = "resnet50_vectors";
    String name = milvusOperateService.getOldCollectionName(alias);
    log.info("[testMilvusName][{}],name : {}", alias, name);
  }

  @Test
  public void rename(){
    String alias = "resnet50_vectors";
    String newName = "resnet50_vectors_1753093046455";
    String oldName = "resnet50_vectors_1752677026883";
    Boolean flag = milvusOperateService.renameAliasToRealCollection(newName,oldName,alias);
    log.info("[rename][{}],flag : {}", alias, flag);
  }

  @Test
  public void loadCollection(){
    String alias = "resnet50_vectors_1753093046455";
    milvusOperateService.loadCollection(alias);
  }

  @Test
  public void testRecall(){

    Map<String,List<Double>> vectorMap = CsvReadVectorUtils.readVector("./task-file/db/355/smallImage/355_7_1_5/355_7_1_5.csv"
        .replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));

    String alias = "resnet50_vectors";
    List<Float> floatList = Lists.newArrayList();
    for (String mdoelName : vectorMap.keySet()) {
      if (ModelNameEnum.ResNet50.getL2VectorName().equals(mdoelName)) {
        floatList = vectorMap.get(mdoelName).stream().map(Double::floatValue).collect(Collectors.toList());
      }
    }
    String collectionName = ModelNameEnum.ResNet50.getCollectionName();
    VectorQueryTypeEnum queryType = VectorQueryTypeEnum.COSINE;
    String strategy = "";
    List<Map<String,Object>>  reusltList = milvusRecallService.recall(floatList,collectionName,queryType,strategy);
    log.info(JSONObject.toJSONString(reusltList));
  }

}
