package cn.iocoder.yudao;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.task.common.TaskImageProcessService;
import cn.iocoder.yudao.module.system.enums.task.FileTypeEnum;
import cn.iocoder.yudao.server.YudaoServerApplication;
import com.alibaba.fastjson.JSONObject;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = YudaoServerApplication.class)
public class TestProcessImage {

  @Resource
  private TaskImageProcessService imageProcessService;

  @Test
  public void TestProcessImage() {
    TenantContextHolder.setTenantId(1L);
    imageProcessService.process(49L, FileTypeEnum.PDF.getCode());

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

}
