package cn.iocoder.yudao.module.system.api.task;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskQueryResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskAllocateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskReviewReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.task.TaskStatusEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.task.PdfParseService;
import cn.iocoder.yudao.module.system.service.task.dto.PdfParseResultDTO;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.util.TimeUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
  private PdfParseService pdfParseService;


  private static final String UPLOAD_PATH = "./task-file/%s";

  public PageResult<ImageTaskQueryResDTO> query(ImageTaskQueryReqVO imageTaskQueryReqVO){
    PageResult<ImageTaskDO> imageTaskDOPageResult = imageTaskService.pageQuery(imageTaskQueryReqVO);
    PageResult<ImageTaskQueryResDTO> pageResult = BeanUtils.toBean(imageTaskDOPageResult, ImageTaskQueryResDTO.class);
    List<ImageTaskQueryResDTO> queryResDTOList = pageResult.getList();
    for (ImageTaskQueryResDTO queryResDTO : queryResDTOList) {

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
      if (queryResDTO.getFileType().equals("pdf")){
        List<ArticleDO> articleDOList = articleService.queryListByTaskId(queryResDTO.getId());
        for (ArticleDO articleDO : articleDOList) {
          articleTitleList.add(articleDO.getArticleTitle());
          articleJournalList.add(articleDO.getArticleJournal());
          fileUrlList.add(articleDO.getFilePath());
        }
      }
      queryResDTO.setFileUrlList(fileUrlList);
      queryResDTO.setArticleTitleList(articleTitleList);
      queryResDTO.setArticleJournalList(articleJournalList);
    }
    return pageResult;
  }



  @Transactional(rollbackFor = Exception.class)
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
    String filePath = String.format(UPLOAD_PATH, String.valueOf(imageTaskDO.getId()));

    // 上传文件
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = uploadFiles(files, filePath);
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

      // 设置基本信息
      articleDO.setArticleDate(System.currentTimeMillis());
      if ("pdf".equalsIgnoreCase(reqVO.getFileType())){
        articleDO.setIsImage(0);
      } else if ("image".equalsIgnoreCase(reqVO.getFileType())){
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

    // 对PDF文件进行异步解析
    if ("pdf".equalsIgnoreCase(reqVO.getFileType())) {
      for (ArticleDO articleDO : articleDOList) {
        asyncParsePdfAndUpdate(articleDO);
      }
    }

    // 更新任务状态为算法检测中
    ImageTaskDO updateImageTaskStatus = new ImageTaskDO();
    updateImageTaskStatus.setId(imageTaskDO.getId());
    updateImageTaskStatus.setTaskStatus(TaskStatusEnum.ALGO_DETECT.getCode());
    imageTaskService.update(updateImageTaskStatus);

    return imageTaskResDTO;
  }

  private ImageTaskCreateResDTO uploadFiles(MultipartFile[] files, String taskFilePath){

    ImageTaskCreateResDTO imageTaskResDTO = new ImageTaskCreateResDTO();
    try {
      // 确保上传目录存在
      File uploadDir = new File(taskFilePath);
      if (!uploadDir.exists()) {
        uploadDir.mkdirs();
      }

      List<String> failedFile = new ArrayList<>();
      List<FileContent> successFile = new ArrayList<>();

      // 上传文件
      for (MultipartFile file : files) {
        String originalFilename = file.getOriginalFilename();
        FileContent fileContent = new FileContent();

        try {

          if (!isValidFileSize(file)) {
            failedFile.add(originalFilename + " (文件过大)");
            imageTaskResDTO.setSuccess(Boolean.FALSE);
            continue;
          }

          // 保存文件
          String fullLocalFilePath = taskFilePath + "/" + originalFilename;
          Path filePath = Paths.get(fullLocalFilePath);
          Files.write(filePath, file.getBytes());
          fileContent.setFileName(originalFilename);
          fileContent.setFilePath(fullLocalFilePath);
          fileContent.setFileSize(file.getSize());
          successFile.add(fileContent);
        } catch (IOException e) {
          failedFile.add(originalFilename + " (上传失败: " + e.getMessage() + ")");
          imageTaskResDTO.setSuccess(Boolean.FALSE);
        }
      }
      imageTaskResDTO.setFailedFile(failedFile);
      imageTaskResDTO.setSuccessFile(successFile);
    }catch (Exception e){
      log.error("uploadFiles error", e);
      imageTaskResDTO.setSuccess(Boolean.FALSE);
      imageTaskResDTO.setFailedMsg(e.getMessage());
    }
    return imageTaskResDTO;
  }

  private boolean isValidFileType(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null) {
      return false;
    }

    // 允许的MIME类型
    return contentType.startsWith("image/") ||
        contentType.equals("application/pdf") ||
        contentType.equals("application/octet-stream"); // 某些PDF可能返回此类型
  }

  private boolean isValidFileSize(MultipartFile file) {
    return file.getSize() <= 10 * 1024 * 1024; // 10MB限制
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

  /**
   * 异步解析PDF并更新文章信息
   */
  private void asyncParsePdfAndUpdate(ArticleDO articleDO) {
    pdfParseService.parsePdfAsync(articleDO.getFilePath())
        .thenAccept(parseResult -> {
          try {
            log.info("PDF解析结果: {}", parseResult);
            if (parseResult.getSuccess() != null && parseResult.getSuccess()) {
              // 解析成功，更新文章信息
              updateArticleWithPdfResult(articleDO, parseResult);
              log.info("PDF解析成功并更新文章信息: {}", articleDO.getFileName());
            } else {
              log.warn("PDF解析失败: {}, 错误信息: {}", articleDO.getFileName(), parseResult.getErrorMessage());
            }
          } catch (Exception e) {
            log.error("更新文章信息失败: {}", articleDO.getFileName(), e);
          }
        })
        .exceptionally(throwable -> {
          log.error("PDF异步解析异常: {}", articleDO.getFileName(), throwable);
          return null;
        });
  }

  /**
   * 根据PDF解析结果更新文章信息
   */
  private void updateArticleWithPdfResult(ArticleDO articleDO, PdfParseResultDTO parseResult) {
    try {
      ArticleDO updateArticle = new ArticleDO();
      updateArticle.setArticleId(articleDO.getArticleId());
      
      // 更新文章标题
      if (parseResult.getTitle() != null) {
        updateArticle.setArticleTitle(parseResult.getTitle());
      }
      
      // 更新杂志名称
      if (parseResult.getJournal() != null) {
        updateArticle.setArticleJournal(parseResult.getJournal());
      }
      
      // 更新关键词列表
      if (parseResult.getKeywords() != null && !parseResult.getKeywords().isEmpty()) {
        updateArticle.setArticleKeywords(parseResult.getKeywords());
      }
      
      // 更新作者姓名列表
      if (parseResult.getAuthors() != null && !parseResult.getAuthors().isEmpty()) {
        updateArticle.setAuthorName(parseResult.getAuthors());
        // 由于API没有返回作者单位信息，暂时设置为空列表
        updateArticle.setAuthorInstitution(Lists.newArrayList());
      }
      
      // 更新发表日期
      if (parseResult.getPublicationDate() != null) {
        try {
          // 假设日期格式为 yyyy-MM-dd，需要转换为时间戳
          java.time.LocalDate date = java.time.LocalDate.parse(parseResult.getPublicationDate());
          updateArticle.setArticleDate(date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
        } catch (Exception e) {
          log.warn("解析发表日期失败: {}", parseResult.getPublicationDate());
        }
      }
      
      // 更新DOI
      if (parseResult.getDoi() != null) {
        updateArticle.setPmid(parseResult.getDoi()); // 暂时将DOI存储在PMID字段
      }

      // 执行更新
      log.info("更新文章信息: {}", updateArticle);
      articleService.update(updateArticle);
      
    } catch (Exception e) {
      log.error("更新文章信息失败", e);
      throw new RuntimeException("更新文章信息失败: " + e.getMessage());
    }
  }

}
