package cn.iocoder.yudao.module.system.service.task;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.service.task.dto.FeaturePointQueryDTO;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QueryFeaturePointService {

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private ImgSimilarityService imgSimilarityService;

  @Resource
  private TaskConfig taskConfig;

  public void queryFeaturePoints(List<ImgSimilarityDO> recallList, Long taskId) {
    if (CollectionUtils.isEmpty(recallList)) {
      return;
    }

    Set<Long> smallImageIdSet = recallList.stream().map(ImgSimilarityDO::getSourceSmallImageId).collect(Collectors.toSet());
    Set<Long> targetImageIdSet = recallList.stream().map(ImgSimilarityDO::getTargetSmallImageId).collect(Collectors.toSet());
    smallImageIdSet.addAll(targetImageIdSet);
    List<SmallImageDO> smallImageDOList = smallImageService.queryByIds(smallImageIdSet);
    Map<Long, SmallImageDO> smallImageDOMap = smallImageDOList.stream().collect(Collectors.toMap(SmallImageDO::getId, x->x));

    int batch = 50;
    List<FeaturePointQueryDTO> batchList = new ArrayList<>();
    for (ImgSimilarityDO recall : recallList) {
      FeaturePointQueryDTO queryDTO = new FeaturePointQueryDTO();
      queryDTO.setSimilarId(recall.getId());
      SmallImageDO smallImageDO = smallImageDOMap.get(recall.getSourceSmallImageId());
      SmallImageDO targetSmall = smallImageDOMap.get(recall.getTargetSmallImageId());
      if (Objects.isNull(smallImageDO) || Objects.isNull(targetSmall)) {
        log.warn("smallImageDO is null or targetSmall is null");
        continue;
      }
      queryDTO.setSmallImage(smallImageDO.getImagePath().replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));
      queryDTO.setDuplicateSmallImage(targetSmall.getImagePath().replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));
      batchList.add(queryDTO);
      if (batchList.size() >= batch) {
        callFeaturePoint(batchList);
        batchList.clear();
      }
    }
    if (CollectionUtils.isNotEmpty(batchList)){
      callFeaturePoint(batchList);
    }

    // 删除特征点位0的图片对
    Integer count = imgSimilarityService.deleteZeroPoints(taskId,0);
    log.info("delete zero points img similarity count : {}", count);
  }

  private void callFeaturePoint(List<FeaturePointQueryDTO> batchList){
    String response = HttpUtils.post(taskConfig.getFeatureImageUrl(),null, JSONObject.toJSONString(batchList));
    Optional<String> featureResultStr = getFeatureResultStr(batchList,response);
    if (!featureResultStr.isPresent()) {
      return;
    }
    Map<String,Integer> featureResult = JSONObject.parseObject(featureResultStr.get(),Map.class);
    List<ImgSimilarityDO> updateList = new ArrayList<>();
    for (String id : featureResult.keySet()) {
      ImgSimilarityDO imgSimilarityDO = new ImgSimilarityDO();
      imgSimilarityDO.setId(Long.parseLong(id));
      imgSimilarityDO.setFeaturePointCnt(featureResult.get(id));
      updateList.add(imgSimilarityDO);
    }
    Boolean flag = imgSimilarityService.updateBatch(updateList);
    if (!flag) {
      log.error("update feature point failed");
    }
  }

  private Optional<String> getFeatureResultStr(List<FeaturePointQueryDTO> batchList, String response) {
    log.info("end call api, request ={}, response = {}", JSONObject.toJSONString(batchList) , response);
    if (StringUtils.isBlank(response)){
      log.error("获取特征点失败，无返回"); // todo 写库
      return Optional.empty();
    }
    JSONObject jsonObject = JSONObject.parseObject(response);
    if (!"0000".equals(jsonObject.getString("code")) ) {
      log.error("获取特征点失败, result : {}", jsonObject.getString("code"));
      return Optional.empty();
    }

    String resultStr = jsonObject.getString("data");
    if (StringUtils.isBlank(resultStr)) {
      log.error("获取特征点失败, result : {}", jsonObject.getString("code"));
      return Optional.empty();
    }
    return Optional.of(resultStr);
  }

}
