package cn.iocoder.yudao.module.system.api.task;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.common.FileUploadService;
import cn.iocoder.yudao.module.system.api.task.common.TaskImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.PdfArticleParseService;
import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskQueryResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskAllocateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskReviewReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.FileTypeEnum;
import cn.iocoder.yudao.module.system.enums.task.TaskStatusEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ImageTaskApiService {

  @Resource
  private ImageTaskService imageTaskService;

  @Resource
  private ArticleService articleService;

  @Resource
  private AdminUserService adminUserService;

  @Resource
  private DeptService deptService;

  @Resource
  private RoleService roleService;

  @Resource
  private PermissionService permissionService;

  @Resource
  private FileUploadService fileUploadService;

  @Resource
  private TaskImageProcessService imageProcessService;


  private static final String UPLOAD_PATH = "./task-file/%s";

  public PageResult<ImageTaskQueryResDTO> query(ImageTaskQueryReqVO imageTaskQueryReqVO){

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

    // 如果是专家 只能看到分配给他的
    if (roleDo.getCode().equals("Expert_admin")){
      imageTaskQueryReqVO.setReviewId(WebFrameworkUtils.getLoginUserId());
    }else if (roleDo.getCode().equals("Common")){
      imageTaskQueryReqVO.setCreatorId(WebFrameworkUtils.getLoginUserId());
    }

    PageResult<ImageTaskDO> imageTaskDOPageResult = imageTaskService.pageQuery(imageTaskQueryReqVO);
    PageResult<ImageTaskQueryResDTO> pageResult = BeanUtils.toBean(imageTaskDOPageResult, ImageTaskQueryResDTO.class);
    List<ImageTaskQueryResDTO> queryResDTOList = pageResult.getList();
    for (ImageTaskQueryResDTO queryResDTO : queryResDTOList) {

      queryResDTO.setRole(roleDo.getCode());

      // 补充创建用户信息
      if (roleDo.getCode().equalsIgnoreCase("super_admin") || roleDo.getCode().equalsIgnoreCase("Research_admin")){
        AdminUserDO createUser = adminUserService.getUser(queryResDTO.getCreatorId());
        if (Objects.nonNull(createUser)) {
          queryResDTO.setUserName(createUser.getUsername());
          DeptDO deptDO = deptService.getDept(createUser.getDeptId());
          if (Objects.nonNull(deptDO)) {
            queryResDTO.setUserUnit(deptDO.getName());
          }
        }
      }


      // 补充审核用户信息
      if (!roleDo.getCode().equalsIgnoreCase("Common")){
        AdminUserDO reviewUser = adminUserService.getUser(queryResDTO.getReviewerId());
        if (Objects.nonNull(reviewUser)) {
          queryResDTO.setReviewUserName(reviewUser.getUsername());
          DeptDO reviewDeptDO = deptService.getDept(reviewUser.getDeptId());
          if (Objects.nonNull(reviewDeptDO)) {
            queryResDTO.setReviewUserUnit(reviewDeptDO.getName());
          }
        }
      }

      // 补充论文标题和杂志社
      List<String> articleTitleList = Lists.newArrayList();
      List<String> articleJournalList = Lists.newArrayList();
      List<String> fileUrlList = Lists.newArrayList();
      List<String> imageList = Lists.newArrayList();
      
      List<ArticleDO> articleDOList = articleService.queryListByTaskId(queryResDTO.getId());
      for (ArticleDO articleDO : articleDOList) {
        if (queryResDTO.getFileType().equals("pdf")){
          articleTitleList.add(articleDO.getArticleTitle());
          articleJournalList.add(articleDO.getArticleJournal());
          fileUrlList.add(articleDO.getFilePath());
        } else {
          imageList.add(articleDO.getFilePath());
        }
      }
      
      queryResDTO.setFileUrlList(fileUrlList);
      queryResDTO.setFirstImage(imageList);
      queryResDTO.setArticleTitleList(articleTitleList);
      queryResDTO.setArticleJournalList(articleJournalList);
    }
    return pageResult;
  }



  //@Transactional(rollbackFor = Exception.class)
  public ImageTaskCreateResDTO createTask(ImageTaskCreateReqVO reqVO){

    // 参数检测
    if (reqVO.getTaskType() == 2) {
      if (Objects.isNull(reqVO.getTaskStrategyConfig()) ||
          (Objects.isNull(reqVO.getTaskStrategyConfig().getStartTime()) && Objects.isNull(reqVO.getTaskStrategyConfig().getEndTime())
          && CollectionUtils.isAnyEmpty(reqVO.getTaskStrategyConfig().getKeywordList())
          && Objects.isNull(reqVO.getTaskStrategyConfig().getMedicalSpecialty()))) {
        throw new RuntimeException("策略查，必须传策略参数");
      }
    }

    // 创建任务
    ImageTaskDO imageTaskDO = new ImageTaskDO();
    imageTaskDO.setCreatorId(WebFrameworkUtils.getLoginUserId());
    imageTaskDO.setTaskType(reqVO.getTaskType());
    imageTaskDO.setFileType(reqVO.getFileType());
    if (reqVO.getTaskType() == 2) {
      imageTaskDO.setStrategyConfig(JSONObject.toJSONString(reqVO.getTaskStrategyConfig()));
    }
    Integer sum = imageTaskService.create(imageTaskDO);
    if (Objects.isNull(sum) || sum < 1){
      throw new RuntimeException("任务入库失败");
    }

    // 任务ID
    String filePath = String.format(UPLOAD_PATH, imageTaskDO.getId());

    // 上传文件
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isAnyEmpty(imageTaskResDTO.getSuccessFile())) {
      return imageTaskResDTO;
    }

    // 更新任务
    List<FileContent> fileList = imageTaskResDTO.getSuccessFile();
    ImageTaskDO updateTask = new ImageTaskDO();
    updateTask.setId(imageTaskDO.getId());
    updateTask.setTotalImages(fileList.size());
    updateTask.setTaskNo("RW"+ LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN)
        + imageTaskDO.getId());
    imageTaskService.update(updateTask);

    // 创建文件并异步解析PDF
    List<ArticleDO> articleDOList = Lists.newArrayList();
    for (FileContent fileContent : fileList) {
      ArticleDO articleDO = new ArticleDO();
      articleDO.setTaskId(imageTaskDO.getId());
      articleDO.setFileName(fileContent.getFileName());
      articleDO.setFilePath(fileContent.getFilePath());
      articleDO.setFileSize(fileContent.getFileSize());
      articleDO.setFileType(reqVO.getFileType());
      articleDO.setIsSource(0);

      // 设置基本信息
      articleDO.setArticleDate(System.currentTimeMillis());
      if (FileTypeEnum.PDF.getCode().equalsIgnoreCase(reqVO.getFileType())){
        articleDO.setIsImage(0);
      } else if (FileTypeEnum.IMAGE.getCode().equalsIgnoreCase(reqVO.getFileType())){
        articleDO.setIsImage(1);
      }

      // 初始化为空列表，等待PDF解析完成后更新
      articleDO.setArticleKeywords(Lists.newArrayList());
      articleDO.setAuthorName(Lists.newArrayList());
      articleDO.setAuthorInstitution(Lists.newArrayList());
      
      // 设置默认值
      articleDO.setArticleJournal("");
      articleDO.setArticleTitle("");
      articleDO.setPmid("");
      articleDO.setMedicalSpecialty("");
      
      articleDOList.add(articleDO);
    }
    Boolean success = articleService.batchCreate(articleDOList);
    if (!success){
      throw new RuntimeException("任务入库失败");
    }

    // 异步算法检测
    log.info("commit async");
    imageProcessService.processAsync(imageTaskDO.getId());

    // 更新任务状态为算法检测中
    ImageTaskDO updateImageTaskStatus = new ImageTaskDO();
    updateImageTaskStatus.setId(imageTaskDO.getId());
    updateImageTaskStatus.setTaskStatus(TaskStatusEnum.ALGO_DETECT.getCode());
    imageTaskService.update(updateImageTaskStatus);

    return imageTaskResDTO;
  }


  public CommonResult<String> allocateTask(ImageTaskAllocateReqVO allocateReqVO) {
    Long id = allocateReqVO.getId();
    if (Objects.isNull(id)) {
      return CommonResult.error(500, "任务id不能为空");
    }
    ImageTaskDO imageTaskDO = imageTaskService.getById(id);
    if (Objects.isNull(imageTaskDO)) {
      return CommonResult.error(500, "任务不存在【" + id + "】");
    }
    ImageTaskDO updateTask = new ImageTaskDO();
    updateTask.setId(id);
    updateTask.setAdminId(WebFrameworkUtils.getLoginUserId());
    updateTask.setReviewerId(allocateReqVO.getAdminId());
    updateTask.setAdminTime(LocalDateTime.now());
    Integer sum = imageTaskService.update(updateTask);
    if (Objects.isNull(sum) || sum < 1){
      return CommonResult.error(500, "任务分配失败，请联系管理员");
    }
    return CommonResult.success("success");
  }

  public CommonResult<String> reviewTask(ImageTaskReviewReqVO reviewReqVO) {

    Long id = reviewReqVO.getId();
    if (Objects.isNull(id)) {
      return CommonResult.error(500, "任务id不能为空");
    }
    ImageTaskDO imageTaskDO = imageTaskService.getById(id);
    if (Objects.isNull(imageTaskDO)) {
      return CommonResult.error(500, "任务不存在【" + id + "】");
    }
    ImageTaskDO updateTask = new ImageTaskDO();
    updateTask.setId(id);
    updateTask.setReviewResult(reviewReqVO.getReviewResult());
    updateTask.setAdminTime(LocalDateTime.now());
    Integer sum = imageTaskService.update(updateTask);
    if (Objects.isNull(sum) || sum < 1){
      return CommonResult.error(500, "任务分配失败，请联系管理员");
    }
    return CommonResult.success("success");
  }



}
