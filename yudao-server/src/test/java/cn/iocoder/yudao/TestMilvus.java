package cn.iocoder.yudao;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.task.common.TaskImageProcessService;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import cn.iocoder.yudao.server.YudaoServerApplication;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest(classes = YudaoServerApplication.class)
public class TestMilvus {

  @Resource
  private MilvusOperateService milvusOperateService;

  @Resource
  private TaskImageProcessService imageProcessService;

  @Test
  public void fullDump() {
    TenantContextHolder.setTenantId(1L);
    milvusOperateService.fullDump("resnet50_vectors", 2048);
  }

  @Test
  public void testData(){
    TenantContextHolder.setTenantId(1L);
    String collectionName = "resnet50_vectors_1752677026883";
    milvusOperateService.batchWriteDataFromDb(collectionName);
  }

  @Test
  public void testWriteVectors(){
    TenantContextHolder.setTenantId(1L);
    String collectionName = "resnet50_vectors_1751209930493";
    milvusOperateService.batchWriteDataFromDb(collectionName);
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
    String alias = "resnet50_vectors_1752677026883";
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
    String newName = "resnet50_vectors_1752677026883";
    String oldName = "resnet50_vectors_1752161613282";
    Boolean flag = milvusOperateService.renameAliasToRealCollection(newName,oldName,alias);
    log.info("[rename][{}],flag : {}", alias, flag);
  }

  @Test
  public void loadCollection(){
    String alias = "resnet50_vectors_1752677026883";
    milvusOperateService.loadCollection(alias);
  }

}
