package cn.iocoder.yudao;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.common.ImageProcessService;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import cn.iocoder.yudao.server.YudaoServerApplication;
import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest(classes = YudaoServerApplication.class)
public class TestMilvus {

  @Resource
  private MilvusOperateService milvusOperateService;

  @Resource
  private ImageProcessService imageProcessService;

  @Test
  public void fullDump() {
    TenantContextHolder.setTenantId(1L);
    milvusOperateService.fullDump("resnet50_vectors", 2048);
  }

  @Test
  public void testData(){
    TenantContextHolder.setTenantId(1L);
    String collectionName = "resnet50_vectors_1752144818416";
    milvusOperateService.batchWriteDataFromDb(collectionName);
  }

  @Test
  public void testWriteVectors(){
    TenantContextHolder.setTenantId(1L);
    String collectionName = "resnet50_vectors_1751209930493";
    milvusOperateService.batchWriteData(collectionName);
  }

  @Test
  public void testDeleteCollection(){
    String collectionName = "resnet50_vectors_1751808797586";
    milvusOperateService.collectionDelete(collectionName);
  }

  @Test
  public void testManagerAlias(){
    String oldName = null;
    String newName = "resnet50_vectors_1752144818416";
    String alias = "resnet50_vectors";
    milvusOperateService.renameAliasToRealCollection(newName, oldName, alias);
  }

  @Test
  public void getMilvusCount(){
    String alias = "resnet50_vectors";
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
    String newName = "resnet50_vectors_1752144818416";
    String oldName = "resnet50_vectors_1751808797586";
    Boolean flag = milvusOperateService.renameAliasToRealCollection(newName,oldName,alias);
    log.info("[rename][{}],flag : {}", alias, flag);
  }

  @Test
  public void loadCollection(){
    String alias = "resnet50_vectors";
    milvusOperateService.loadCollection(alias);
  }

}
