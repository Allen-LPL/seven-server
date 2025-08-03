package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.config.TaskConfig;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultFeaturePointsDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultImageTypeDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultModelDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarCompareResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarDefaultResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityReviewReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCompleteReviewReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.TaskSearchPreferencesDO;
import cn.iocoder.yudao.module.system.enums.task.FeaturePointsEnum;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.ImageTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.enums.task.TaskStatusEnum;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.task.TaskSearchPreferencesService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.module.system.enums.task.SimilarLevelEnum;

@Service
@Slf4j
public class ImgSimilarApiService {

  @Resource
  private AdminUserService adminUserService;

  @Resource
  private RoleService roleService;

  @Resource
  private PermissionService permissionService;

  @Resource
  private ImgSimilarityService imgSimilarityService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private ImageTaskService imageTaskService;

  @Resource
  private TaskConfig taskConfig;

  @Resource
  private TaskSearchPreferencesService taskSearchPreferencesService;


  public PageResult<ImgSimilarQueryResVO> query(ImgSimilarityQueryReqVO reqVO){

    // 保存搜索偏好
    if (Objects.nonNull(reqVO.getTaskId())) {
        taskSearchPreferencesService.saveSearchPreferences(reqVO);
    }

    // 当前用户角色
    AdminUserDO adminUserDO = adminUserService.getUser(WebFrameworkUtils.getLoginUserId());
    if (Objects.isNull(adminUserDO)) {
      throw new RuntimeException("用户未登录");
    }
    List<RoleDO> userRoles = roleService.getRoleListFromCache(permissionService.getUserRoleIdListByUserId(adminUserDO.getId()));
    if (CollectionUtils.isEmpty(userRoles)){
      throw new RuntimeException("用户未分配角色");
    }
    RoleDO roleDo = userRoles.get(0);

    if (CollectionUtils.isEmpty(reqVO.getModelNameList())){
      reqVO.setModelNameList(Lists.newArrayList(ModelNameEnum.DenseNet121.getCode()));
    }
    if (Objects.nonNull(reqVO.getSimilarScoreThreshold())){
      reqVO.setSimilarScoreThreshold(reqVO.getSimilarScoreThreshold()/100);
    }
    PageResult<ImgSimilarityDO> imageTaskDOPageResult = imgSimilarityService.pageResult(reqVO);
    PageResult<ImgSimilarQueryResVO> pageResult = BeanUtils.toBean(imageTaskDOPageResult, ImgSimilarQueryResVO.class);
    List<ImgSimilarQueryResVO> queryResDTOList = pageResult.getList();
    for (ImgSimilarQueryResVO imgSimilarQueryResVO : queryResDTOList) {


      // 补充创建用户信息
      AdminUserDO creatorUser = adminUserService.getUser(Long.parseLong(imgSimilarQueryResVO.getCreator()));
      if (Objects.nonNull(creatorUser)) {
        imgSimilarQueryResVO.setCreatorUserName(creatorUser.getUsername());
      }

      // 补充审核用户信息
      AdminUserDO reviewUser = adminUserService.getUser(imgSimilarQueryResVO.getReviewerId());
      if (Objects.nonNull(reviewUser)) {
        imgSimilarQueryResVO.setReviewerUserName(reviewUser.getUsername());
      }

      // 小图url
      SmallImageDO sourceSmall = smallImageService.queryById(imgSimilarQueryResVO.getSourceSmallImageId());
      if (Objects.nonNull(sourceSmall)) {
        imgSimilarQueryResVO.setSourceSmallImagePath(sourceSmall.getImagePath());
        imgSimilarQueryResVO.setImageType(Collections.singletonList(sourceSmall.getImageType()));
      }
      SmallImageDO targetSmall = smallImageService.queryById(imgSimilarQueryResVO.getTargetSmallImageId());
      if (Objects.nonNull(targetSmall)) {
        imgSimilarQueryResVO.setTargetSmallImagePath(targetSmall.getImagePath());
      }

      // 相似level
      Integer featurePoints = imgSimilarQueryResVO.getFeaturePointCnt();
      if (Objects.isNull(featurePoints)) {
        featurePoints = 0;
      }
      if (featurePoints>=1 && featurePoints<=5){
        imgSimilarQueryResVO.setSimilarityLevel(SimilarLevelEnum.light.getLevel());
      }else if (featurePoints>=6 && featurePoints<26){
        imgSimilarQueryResVO.setSimilarityLevel(SimilarLevelEnum.middle.getLevel());
      }else if (featurePoints >= 26){
        imgSimilarQueryResVO.setSimilarityLevel(SimilarLevelEnum.weight.getLevel());
      }
      imgSimilarQueryResVO.setSimilarityScore(imgSimilarQueryResVO.getSimilarityScore()*100);
    }
    return pageResult;
  }



  public CommonResult<String> reviewSimilar(ImgSimilarityReviewReqVO reviewReqVO) {

    Long id = reviewReqVO.getId();
    if (Objects.isNull(id)) {
      return CommonResult.error(500, "相似对id不能为空");
    }
    ImgSimilarityDO imgSimilarityDO = imgSimilarityService.getById(id);
    if (Objects.isNull(imgSimilarityDO)) {
      return CommonResult.error(500, "相似对不存在【" + id + "】");
    }
    ImgSimilarityDO updateImgSimilar = new ImgSimilarityDO();
    updateImgSimilar.setId(id);
    if (StringUtils.isNotBlank(reviewReqVO.getReviewComment())){
      updateImgSimilar.setReviewComment(reviewReqVO.getReviewComment());
    }
   if (Objects.nonNull(reviewReqVO.getIsSimilar())){
     updateImgSimilar.setIsSimilar(reviewReqVO.getIsSimilar());
   }
    updateImgSimilar.setReviewTime(LocalDateTime.now());
    Integer sum = imgSimilarityService.updateById(updateImgSimilar);
    if (Objects.isNull(sum) || sum < 1){
      return CommonResult.error(500, "审核失败，请联系管理员");
    }
    return CommonResult.success("success");
  }

  /**
   * 完成审核
   *
   * @param reqVO 请求参数
   * @return 操作结果
   */
  public CommonResult<String> completeReview(ImageTaskCompleteReviewReqVO reqVO) {
    Long taskId = reqVO.getTaskId();
    if (Objects.isNull(taskId)) {
      return CommonResult.error(500, "任务ID不能为空");
    }
    ImageTaskDO imageTaskDO = imageTaskService.getById(taskId);
    if (Objects.isNull(imageTaskDO)) {
      return CommonResult.error(500, "任务不存在【" + taskId + "】");
    }

    ImageTaskDO updateImageTask = new ImageTaskDO();
    updateImageTask.setId(taskId);
    updateImageTask.setReviewResult(reqVO.getReviewResult());
    updateImageTask.setTaskStatus(TaskStatusEnum.COMPLETE.getCode()); // 设置为审核完成状态
    updateImageTask.setReviewTime(LocalDateTime.now());
    updateImageTask.setUpdater(String.valueOf(WebFrameworkUtils.getLoginUserId()));

    imageTaskService.update(updateImageTask);
    return CommonResult.success("success");
  }

  public CommonResult<ImgSimilarCompareResVO> compare(Long id){

    if (Objects.isNull(id)) {
      return CommonResult.error(500, "参数不能为空");
    }

    ImgSimilarityDO imgSimilarityDO = imgSimilarityService.getById(id);
    if (Objects.isNull(imgSimilarityDO)) {
      return CommonResult.error(500, "相似对为空");
    }

    if (StringUtils.isNotBlank(imgSimilarityDO.getDotImage()) && StringUtils.isNotBlank(imgSimilarityDO.getBlockImage())){
      ImgSimilarCompareResVO resVO = new ImgSimilarCompareResVO();
      resVO.setBlockImage(imgSimilarityDO.getBlockImage());
      resVO.setDotImage(imgSimilarityDO.getDotImage());
      return CommonResult.success(resVO);
    }

    SmallImageDO sourceSmallImageDO = smallImageService.queryById(imgSimilarityDO.getSourceSmallImageId());
    if (Objects.isNull(sourceSmallImageDO)) {
      return CommonResult.error(500,"原小图为空");
    }

    SmallImageDO targetSmallImageDO = smallImageService.queryById(imgSimilarityDO.getTargetSmallImageId());
    if (Objects.isNull(targetSmallImageDO)) {
      return CommonResult.error(500,"目标小图为空");
    }

    JSONObject params = new JSONObject();
    params.put("smallImage",sourceSmallImageDO.getImagePath().replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));
    params.put("duplicateSmallImage",targetSmallImageDO.getImagePath().replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));
    String path = String.format(FilePathConstant.COMPARE_LOCAL_PATH, imgSimilarityDO.getTaskId());
    params.put("comparePath",path.replace(FilePathConstant.local_prefix, taskConfig.getReplacePrefix()));
    log.info("compare image params {}", params.toJSONString());
    String result = HttpUtils.post(taskConfig.getCompareImageUrl(),null, params.toJSONString());
    log.info("compare image result: {}", result);

    if (StringUtils.isEmpty(result)){
      return CommonResult.error(500, "");
    }
    JSONObject jsonObject = JSONObject.parseObject(result);
    String codeStr = jsonObject.getString("code");
    if (StringUtils.isEmpty(codeStr) || !"0000".equals(codeStr)){
      log.error(result);
      return CommonResult.error(500, "");
    }

    JSONObject dataObj = jsonObject.getJSONObject("data");
    if (Objects.isNull(dataObj)){
      return CommonResult.error(500, "");
    }
    String  blockImage = dataObj.getString("blockImage");
    String dotImage = dataObj.getString("dotImage");

    ImgSimilarityDO update = new ImgSimilarityDO();
    update.setId(id);
    update.setDotImage(dotImage.replace(taskConfig.getReplacePrefix(),FilePathConstant.local_prefix ));
    update.setBlockImage(blockImage.replace(taskConfig.getReplacePrefix(),FilePathConstant.local_prefix ));
    imgSimilarityService.updateById(update);

    ImgSimilarCompareResVO resVO = new ImgSimilarCompareResVO();
    resVO.setBlockImage(blockImage.replace(taskConfig.getReplacePrefix(),FilePathConstant.local_prefix ));
    resVO.setDotImage(dotImage.replace(taskConfig.getReplacePrefix(),FilePathConstant.local_prefix ));
    return CommonResult.success(resVO);
  }
  
  /**
   * 删除审核意见
   *
   * @param id 相似对id
   * @return 操作结果
   */
  public CommonResult<String> deleteComment(Long id) {
    if (Objects.isNull(id)) {
      return CommonResult.error(500, "相似对id不能为空");
    }
    
    ImgSimilarityDO imgSimilarityDO = imgSimilarityService.getById(id);
    if (Objects.isNull(imgSimilarityDO)) {
      return CommonResult.error(500, "相似对不存在【" + id + "】");
    }
    
    ImgSimilarityDO updateImgSimilar = new ImgSimilarityDO();
    updateImgSimilar.setId(id);
    updateImgSimilar.setReviewComment(""); // 清空审核意见
    
    Integer sum = imgSimilarityService.updateById(updateImgSimilar);
    if (Objects.isNull(sum) || sum < 1) {
      return CommonResult.error(500, "删除审核意见失败，请联系管理员");
    }
    return CommonResult.success("删除审核意见成功");
  }

  public CommonResult<ImgSimilarDefaultResVO> queryDefault(Long taskId) {
    ImgSimilarDefaultResVO resVO = new ImgSimilarDefaultResVO();

    // 获取当前用户角色
    AdminUserDO adminUserDO = adminUserService.getUser(WebFrameworkUtils.getLoginUserId());
    if (adminUserDO != null) {
        List<RoleDO> userRoles = roleService.getRoleListFromCache(permissionService.getUserRoleIdListByUserId(adminUserDO.getId()));
        if (CollectionUtils.isNotEmpty(userRoles)) {
            resVO.setRoles(userRoles.stream().map(RoleDO::getCode).collect(Collectors.toList()));
        }
    }

    populateDefaults(resVO);

    // 尝试获取已保存的偏好设置
    if (Objects.nonNull(taskId)) {
        TaskSearchPreferencesDO preferences = taskSearchPreferencesService.getSearchPreferences(taskId);
        if (Objects.nonNull(preferences)) {
            // 如果存在偏好，则使用偏好设置
            if (StringUtils.isNotBlank(preferences.getModelName())) {
                resVO.getDefaultModelList().forEach(dto -> dto.setSelected(dto.getName().equals(preferences.getModelName())));
                resVO.getDefaultModelList().sort(Comparator.comparing(dto -> !dto.getName().equals(preferences.getModelName())));
                if (Objects.nonNull(preferences.getSimilarScoreThreshold())) {
                    resVO.getDefaultModelList().stream()
                        .filter(dto -> dto.getName().equals(preferences.getModelName()))
                        .findFirst()
                        .ifPresent(dto -> dto.setScore(preferences.getSimilarScoreThreshold()));
                }
            }
            if (StringUtils.isNotBlank(preferences.getImageTypes())) {
                List<String> preferredTypes = JSON.parseArray(preferences.getImageTypes(), String.class);
                resVO.getDefaultImageTypeList().forEach(dto -> dto.setSelected(preferredTypes.contains(dto.getCode())));
                resVO.getDefaultImageTypeList().sort(Comparator.comparing(dto -> !preferredTypes.contains(dto.getCode())));
            }
            if (Objects.nonNull(preferences.getFeaturePoints())) {
                resVO.getDefaultFeaturePointsList().forEach(dto -> dto.setSelected(dto.getValue().equals(preferences.getFeaturePoints())));
                resVO.getDefaultFeaturePointsList().sort(Comparator.comparing(dto -> !dto.getValue().equals(preferences.getFeaturePoints())));
            }
        }
    }

    return CommonResult.success(resVO);
  }

  private void populateDefaults(ImgSimilarDefaultResVO resVO) {
    // 算法
    List<DefaultModelDTO> defaultModelDTOList = Lists.newArrayList();
    for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
        DefaultModelDTO defaultModelDTO = new DefaultModelDTO();
        defaultModelDTO.setName(modelNameEnum.getCode());
        defaultModelDTO.setScore(modelNameEnum.getScore()*100);
        defaultModelDTO.setSelected(false);
        defaultModelDTOList.add(defaultModelDTO);
    }
    resVO.setDefaultModelList(defaultModelDTOList);

    // 图像分类
    List<DefaultImageTypeDTO> imageTypeDTOS = Lists.newArrayList();
    for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
        DefaultImageTypeDTO defaultImageTypeDTO = new DefaultImageTypeDTO();
        defaultImageTypeDTO.setCode(imageTypeEnum.getCode());
        defaultImageTypeDTO.setName(imageTypeEnum.getDesc());
        defaultImageTypeDTO.setSelected(false);
        imageTypeDTOS.add(defaultImageTypeDTO);
    }
    resVO.setDefaultImageTypeList(imageTypeDTOS);

    // 特征点
    List<DefaultFeaturePointsDTO> featurePointsDTOS = Lists.newArrayList();
    for (FeaturePointsEnum featurePointsEnum: FeaturePointsEnum.values()){
        DefaultFeaturePointsDTO defaultFeaturePointsDTO = new DefaultFeaturePointsDTO();
        defaultFeaturePointsDTO.setName(featurePointsEnum.getCode());
        defaultFeaturePointsDTO.setValue(featurePointsEnum.getThreshold());
        defaultFeaturePointsDTO.setSelected(false);
        featurePointsDTOS.add(defaultFeaturePointsDTO);
    }
    resVO.setDefaultFeaturePointsList(featurePointsDTOS);
  }
}
