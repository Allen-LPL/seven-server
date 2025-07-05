package cn.iocoder.yudao.module.system.api.task;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.common.FileUploadService;
import cn.iocoder.yudao.module.system.api.task.common.ImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.PdfArticleParseService;
import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskQueryResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarCompareResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityReviewReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskAllocateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskReviewReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgSimilarityDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.TaskStatusEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ImgSimilarApiService {

  @Resource
  private ImageTaskService imageTaskService;

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

  private static final String compare_url = "http://172.20.76.8:8087/compare_images";

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

    SmallImageDO sourceSmallImageDO = smallImageService.queryById(imgSimilarityDO.getSourceSmallImageId());
    if (Objects.isNull(sourceSmallImageDO)) {
      return CommonResult.error(500,"原小图为空");
    }

    SmallImageDO targetSmallImageDO = smallImageService.queryById(imgSimilarityDO.getTargetSmallImageId());
    if (Objects.isNull(targetSmallImageDO)) {
      return CommonResult.error(500,"目标小图为空");
    }

    JSONObject params = new JSONObject();
    params.put("smallImage",sourceSmallImageDO.getImagePath());
    params.put("duplicateSmallImage",targetSmallImageDO.getImagePath());
    String path = "./task-file/"+ imgSimilarityDO.getTaskId() + "/comparePath/";
    params.put("comparePath",path);
    String result = HttpUtils.post(compare_url,null, params.toJSONString());
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

    ImgSimilarCompareResVO resVO = new ImgSimilarCompareResVO();
    resVO.setBlockImage(blockImage);
    resVO.setDotImage(dotImage);
    return CommonResult.success(resVO);
  }

}
