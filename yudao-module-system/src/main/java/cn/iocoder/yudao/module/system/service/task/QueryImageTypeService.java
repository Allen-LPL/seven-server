package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.LargeImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.service.task.dto.FeaturePointQueryDTO;
import cn.iocoder.yudao.module.system.service.task.dto.ImageTypeQueryDTO;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QueryImageTypeService {

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private TaskConfig taskConfig;

  public void queryImageType(List<LargeImageDO> largeImageDOList, List<SmallImageDO> smallImageDOList) {

    Map<Long,List<SmallImageDO>> largeSmallImageMap = smallImageDOList.stream().collect(Collectors.groupingBy(SmallImageDO::getLargeImageId));

    List<ImageTypeQueryDTO> batchList = Lists.newArrayList();
    int batch = 50;
    for (LargeImageDO largeImage : largeImageDOList) {
      ImageTypeQueryDTO imageTypeQueryDTO = new ImageTypeQueryDTO();
      imageTypeQueryDTO.setFilePath(largeImage.getImagePath().replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));
      imageTypeQueryDTO.setId(largeImage.getId());
      batchList.add(imageTypeQueryDTO);
      if (batchList.size() >= batch){
        callImageType(batchList, largeSmallImageMap);
        batchList.clear();
      }
    }
    if (CollectionUtils.isNotEmpty(batchList)){
      callImageType(batchList, largeSmallImageMap);
    }

  }

  private void callImageType(List<ImageTypeQueryDTO> batchList,Map<Long,List<SmallImageDO>> largeSmallImageMap){
    String response = HttpUtils.post(taskConfig.getClassifyImageUrl(),null, JSONObject.toJSONString(batchList));
    Optional<String> imageTypeResultStr = getImageTypeResultStr(batchList,response);
    if (!imageTypeResultStr.isPresent()) {
      return;
    }
    List<String> imageTypeResult = JSONObject.parseArray(imageTypeResultStr.get(),String.class);
    List<SmallImageDO> updateSmallList = new ArrayList<>();
    List<LargeImageDO> updateLargeList = new ArrayList<>();
    for (int i=0;i<batchList.size();i++) {

      String type = imageTypeResult.get(i);
      ImageTypeQueryDTO imageTypeQueryDTO = batchList.get(i);

      LargeImageDO largeImageDO = new LargeImageDO();
      largeImageDO.setId(imageTypeQueryDTO.getId());
      largeImageDO.setImageType(type);
      updateLargeList.add(largeImageDO);

      List<SmallImageDO> smallImageDOList = largeSmallImageMap.getOrDefault(imageTypeQueryDTO.getId(),Lists.newArrayList());
      smallImageDOList.stream().forEach(smallImageDO -> {
        SmallImageDO update = new SmallImageDO();
        update.setId(smallImageDO.getId());
        update.setImageType(type);
        updateSmallList.add(update);
      });
    }
    Boolean flag = largeImageService.updateBatch(updateLargeList);
    if (!flag) {
      log.error("update feature point failed");
    }
    flag = smallImageService.updateBatch(updateSmallList);
    if (!flag) {
      log.error("update feature point failed");
    }
  }

  private Optional<String> getImageTypeResultStr(List<ImageTypeQueryDTO> batchList, String response) {
    log.info("end call image type api, request ={}, response = {}", JSONObject.toJSONString(batchList) , response);
    if (StringUtils.isBlank(response)){
      log.error("获取图像类型失败，无返回"); // todo 写库
      return Optional.empty();
    }
    JSONObject jsonObject = JSONObject.parseObject(response);
    if (!"0000".equals(jsonObject.getString("code")) ) {
      log.error("获取图像类型失败, result : {}", jsonObject.getString("code"));
      return Optional.empty();
    }

    String resultStr = jsonObject.getString("data");
    if (StringUtils.isBlank(resultStr)) {
      log.error("获取图像类型失败, result : {}", jsonObject.getString("code"));
      return Optional.empty();
    }
    return Optional.of(resultStr);
  }

}
