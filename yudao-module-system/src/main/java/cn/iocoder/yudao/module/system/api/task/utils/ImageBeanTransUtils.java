package cn.iocoder.yudao.module.system.api.task.utils;

import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageRequest;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage;
import cn.iocoder.yudao.module.system.api.task.dto.ProcessImageResponse.LargeImage.SmallImage;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class ImageBeanTransUtils {

  public static LargeImageDO transLargeImageDO(LargeImage largeImage, Long articleId, String replacePrefix,
      String local_prefix){
    LargeImageDO largeImageDO = new LargeImageDO();
    largeImageDO.setIsProcessed(1);
    largeImageDO.setImagePath(largeImage.getPath().replace(replacePrefix,local_prefix));
    largeImageDO.setImageFormat("jpg");
    largeImageDO.setArticleId(articleId);
    largeImageDO.setCaption(largeImage.getCaption());
    largeImageDO.setPageNumber(largeImage.getPage_number());
    largeImageDO.setCaption(largeImage.getCaption());
    File tmpfile = new File(largeImage.getPath());
    largeImageDO.setImageFileName(tmpfile.getName());
    largeImageDO.setImageSize(tmpfile.length()/1024); //kb
    return largeImageDO;
  }

  public static SmallImageDO transSmallImageDO(SmallImage smallImage, Long articleId, Long largeImageId,
      String replacePrefix, String local_prefix){
    String path = smallImage.getPath().replace(replacePrefix,local_prefix);
    String name = path.substring(path.lastIndexOf("/")+1);
    log.info("path={},name={}", path, name);
    String imageUrl = path + "/" + name + ".jpg";
    String vectorPath = path + "/" + name + ".csv";
    SmallImageDO smallImageDO = new SmallImageDO();
    smallImageDO.setArticleId(articleId);
    smallImageDO.setLargeImageId(largeImageId);
    File imageFile = new File(imageUrl);
    smallImageDO.setImageSize(imageFile.length()/1024);
    smallImageDO.setImageType("small");
    smallImageDO.setImageName(name + ".jpg");
    smallImageDO.setCreator(String.valueOf(WebFrameworkUtils.getLoginUserId()));
    smallImageDO.setImagePath(imageUrl);
    smallImageDO.setVectorPath(vectorPath);
    smallImageDO.setCreator("1");
    return smallImageDO;
  }

  public static List<ProcessImageRequest> getProcessImageRequests(Long taskId, List<ArticleDO> articleDOList,String replacePrefix,
      String local_prefix, String LARGE_PATH, String SMALL_PATH) {
    List<ProcessImageRequest> request = Lists.newArrayList();
    for (ArticleDO articleDO : articleDOList) {
      ProcessImageRequest imageRequest = new ProcessImageRequest();
      imageRequest.setArticleId(articleDO.getId());
      imageRequest.setFilePath(articleDO.getFilePath().replace(local_prefix,replacePrefix));
      imageRequest.setFileType(articleDO.getFileType());
      imageRequest.setLargePrefixPath(String.format(LARGE_PATH, replacePrefix, taskId));
      imageRequest.setSmallPrefixPath(String.format(SMALL_PATH, replacePrefix, taskId));
      request.add(imageRequest);
    }
    return request;
  }

  public static Optional<String> getImageCutResultStr(Long taskId, List<ProcessImageRequest> request, String response) {
    log.info("end call api, request ={}, response = {}", JSONObject.toJSONString(request) , response);
    if (StringUtils.isBlank(response)){
      log.error("任务失败，无返回，{}", taskId); // todo 写库
      return Optional.empty();
    }
    JSONObject jsonObject = JSONObject.parseObject(response);
    if (!"0000".equals(jsonObject.getString("code")) ) {
      log.error("任务失败,taskId={}, result : {}", taskId, jsonObject.getString("code"));
      return Optional.empty();
    }

    String resultStr = jsonObject.getString("data");
    if (StringUtils.isBlank(resultStr)) {
      log.error("任务失败,taskId={}, result : {}", taskId, jsonObject.getString("code"));
      return Optional.empty();
    }
    return Optional.of(resultStr);
  }
}
