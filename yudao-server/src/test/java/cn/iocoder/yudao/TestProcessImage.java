package cn.iocoder.yudao;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.task.common.DbImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.TaskImageProcessService;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.enums.task.FileTypeEnum;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.QueryFeaturePointService;
import cn.iocoder.yudao.server.YudaoServerApplication;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = YudaoServerApplication.class)
public class TestProcessImage {

  @Resource
  private TaskImageProcessService imageProcessService;

  @Resource
  private TaskConfig taskConfig;

  @Resource
  private DbImageProcessService dbImageProcessService;

  @Resource
  private ArticleService articleService;

  @Resource
  private QueryFeaturePointService queryFeaturePointService;

  @Resource
  private ImgSimilarityService imgSimilarityService;

  @Test
  public void TestProcessImage() {
    TenantContextHolder.setTenantId(1L);
    imageProcessService.process(49L);
  }

  @Test
  public void testCompareImage() {
    String url = "http://localhost:8087/compare_images";
    TenantContextHolder.setTenantId(1L);
    JSONObject params = new JSONObject();
    params.put("smallImage","/Users/fangliu/Code/image_similar/seven-server/task-file/49/largeImage/158_1_1.jpg");
    params.put("duplicateSmallImage","/Users/fangliu/Code/image_similar/seven-server/task-file/49/smallImage/158_1_1_1.jpg");
    params.put("comparePath","/Users/fangliu/Code/image_similar/seven-server/task-file/49/comparePath/");
    String result = HttpUtils.post(url,null, params.toJSONString());
    log.info("compare image result: {}", result);
  }

  @Test
  public void classifyImage() {
    TenantContextHolder.setTenantId(1L);
    JSONArray params = new JSONArray();
    JSONObject param = new JSONObject();
    param.put("filePath","/Users/fangliu/Code/image_similar/seven-server/task-file/49/smallImage/158_1_1_1.jpg");
    params.add(param);
    String result = HttpUtils.post(taskConfig.getClassifyImageUrl(),null, params.toJSONString());
    log.info("compare image result: {}", result);
  }

  @Test
  public void repeatHandleDbImage(){
    TenantContextHolder.setTenantId(1L);
    Long maxId = 377L;
    Long minId = 355L;
    for (Long articleId = minId; articleId <= maxId; articleId++) {
      dbImageProcessService.repeatProcessFileSingle(articleId);
    }
  }

  @Test
  public void repeatHandleAllImage(){
    TenantContextHolder.setTenantId(1L);
    Long maxId = articleService.maxId();
    Long minId = articleService.minId();
    for (Long articleId = minId; articleId <= maxId; articleId++) {
      dbImageProcessService.repeatProcessFileSingle(articleId);
    }
  }

  @Test
  public void repeatHandleArticle(){
    TenantContextHolder.setTenantId(1L);
    String filePath = "/Users/fangliu/Documents/pdf/";
    String fileType = "pdf";
    dbImageProcessService.batchHandleFileParentDirectory(filePath,fileType);
  }

  @Test
  public void testCompareCount(){
    TenantContextHolder.setTenantId(1L);
    Long taskId = 97L;
    List<ImgSimilarityDO> imgSimilarityDOS =  imgSimilarityService.queryByTaskId(taskId);
    queryFeaturePointService.queryFeaturePoints(imgSimilarityDOS,taskId);
  }

  @Test
  public void testImageClassify(){

  }


}
