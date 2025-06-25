package cn.iocoder.yudao.module.system.api.task.common;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageRequest;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImageProcessService {

  @Resource
  private ArticleService articleService;

  @Resource
  private ImageTaskService imageTaskService;

  private static final String LARGE_PATH = "/Users/fangliu/Code/image_similar/seven-server/task-file/%s/largeImage/";
  private static final String SMALL_PATH = "/Users/fangliu/Code/image_similar/seven-server/task-file/%s/smallImage/";



  private static final String url = "http://localhost:8080/process_articles";


  public void process(Long taskId) {
    List<ArticleDO> articleDOList  = articleService.queryListByTaskId(taskId);
    List<ProcessImageRequest> request = Lists.newArrayList();
    for (ArticleDO articleDO : articleDOList) {
      ProcessImageRequest imageRequest = new ProcessImageRequest();
      imageRequest.setArticleId(articleDO.getId());
      imageRequest.setFilePath(articleDO.getFilePath().replace("./task-file","/Users/fangliu/Code/image_similar/seven-server/task-file"));
      imageRequest.setFileType(articleDO.getFileType());
      imageRequest.setLargePrefixPath(String.format(LARGE_PATH, taskId));
      imageRequest.setSmallPrefixPath(String.format(SMALL_PATH, taskId));
      request.add(imageRequest);
    }
    String response = HttpUtils.post(url,null, JSONObject.toJSONString(request));
    log.info(response);
  }

}
