package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
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
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.FeaturePointsEnum;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
import cn.iocoder.yudao.module.system.enums.task.ImageTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
  private TaskConfig taskConfig;


  public PageResult<ImgSimilarQueryResVO> query(ImgSimilarityQueryReqVO reqVO){

    // 当前用户角色
    AdminUserDO adminUserDO = adminUserService.getUser(WebFrameworkUtils.getLoginUserId());
    if (Objects.isNull(adminUserDO)) {
      throw new RuntimeException("用户未登录");
    }
    List<RoleDO> userRoles = roleService.getRoleListFromCache(permissionService.getUserRoleIdListByUserId(adminUserDO.getId()));
    if (CollectionUtils.isAnyEmpty(userRoles)){
      throw new RuntimeException("用户未分配角色");
    }
    RoleDO roleDo = userRoles.get(0);

    if (CollectionUtils.isAnyEmpty(reqVO.getModelNameList())){
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
      }
      SmallImageDO targetSmall = smallImageService.queryById(imgSimilarQueryResVO.getTargetSmallImageId());
      if (Objects.nonNull(targetSmall)) {
        imgSimilarQueryResVO.setTargetSmallImagePath(targetSmall.getImagePath());
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

  public CommonResult<ImgSimilarDefaultResVO> queryDefault() {
    ImgSimilarDefaultResVO resVO = new ImgSimilarDefaultResVO();
    // 算法
    List<DefaultModelDTO> defaultModelDTOList = Lists.newArrayList();
    for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
      DefaultModelDTO defaultModelDTO = new DefaultModelDTO();
      defaultModelDTO.setName(modelNameEnum.getCode());
      defaultModelDTO.setScore(modelNameEnum.getScore()*100);
      defaultModelDTOList.add(defaultModelDTO);
    }
    resVO.setDefaultModelList(defaultModelDTOList);

    // 图像分类
    List<DefaultImageTypeDTO> imageTypeDTOS = Lists.newArrayList();
    for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
      DefaultImageTypeDTO defaultImageTypeDTO = new DefaultImageTypeDTO();
      defaultImageTypeDTO.setCode(imageTypeEnum.getCode());
      defaultImageTypeDTO.setName(imageTypeEnum.getDesc());
      imageTypeDTOS.add(defaultImageTypeDTO);
    }
    resVO.setDefaultImageTypeList(imageTypeDTOS);

    // 特征点
    List<DefaultFeaturePointsDTO> featurePointsDTOS = Lists.newArrayList();
    for (FeaturePointsEnum featurePointsEnum: FeaturePointsEnum.values()){
      DefaultFeaturePointsDTO defaultFeaturePointsDTO = new DefaultFeaturePointsDTO();
      defaultFeaturePointsDTO.setName(featurePointsEnum.getCode());
      defaultFeaturePointsDTO.setValue(featurePointsEnum.getThreshold());
      featurePointsDTOS.add(defaultFeaturePointsDTO);
    }
    resVO.setDefaultFeaturePointsList(featurePointsDTOS);
    return CommonResult.success(resVO);
  }
}
