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
import cn.iocoder.yudao.module.system.controller.admin.task.vo.adminUser.AdminUserVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskAllocateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskReviewReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserInfoServiceImpl;
import cn.iocoder.yudao.module.system.enums.task.FilePathConstant;
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
import com.google.common.collect.Maps;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

  @Resource
  private AdminUserInfoServiceImpl userInfoService;

  public CommonResult<ImageTaskQueryResDTO> get(ImageTaskQueryReqVO imageTaskQueryReqVO) {

    // 当前用户角色
    AdminUserDO adminUserDO = adminUserService.getUser(WebFrameworkUtils.getLoginUserId());
    if (Objects.isNull(adminUserDO)) {
      throw new RuntimeException("用户未登录");
    }
    List<RoleDO> userRoles = roleService.getRoleListFromCache(permissionService.getUserRoleIdListByUserId(adminUserDO.getId()));
    if (CollectionUtils.isAnyEmpty(userRoles)) {
      throw new RuntimeException("用户未分配角色");
    }
    RoleDO roleDo = userRoles.get(0);

    // 如果是专家 只能看到分配给他的
    if (roleDo.getCode().equals("Expert_admin")) {
      imageTaskQueryReqVO.setReviewId(WebFrameworkUtils.getLoginUserId());
    } else if (roleDo.getCode().equals("Common")) {
      imageTaskQueryReqVO.setCreatorId(WebFrameworkUtils.getLoginUserId());
    }

    ImageTaskDO imageTaskDO = imageTaskService.getById(imageTaskQueryReqVO.getTaskId());
    ImageTaskQueryResDTO queryResDTO = BeanUtils.toBean(imageTaskDO, ImageTaskQueryResDTO.class);
    queryResDTO.setRole(roleDo.getCode());

    // 补充创建用户信息
    //      if (roleDo.getCode().equalsIgnoreCase("super_admin") || roleDo.getCode().equalsIgnoreCase("Research_admin")){
    AdminUserDO createUser = adminUserService.getUser(queryResDTO.getCreatorId());
    if (Objects.nonNull(createUser)) {
      queryResDTO.setUserName(createUser.getNickname());
      DeptDO deptDO = deptService.getDept(createUser.getDeptId());
      if (Objects.nonNull(deptDO)) {
        queryResDTO.setUserUnit(deptDO.getName());
      }
    }
    //      }


    // 补充审核用户信息
    if (queryResDTO.getReviewerId() != null) {
      AdminUserVO reviewUser = userInfoService.getUserById(queryResDTO.getReviewerId());
      if (Objects.nonNull(reviewUser)) {
        queryResDTO.setReviewUserName(reviewUser.getUsername());
        DeptDO reviewDeptDO = deptService.getDept(reviewUser.getDeptId());
        if (Objects.nonNull(reviewDeptDO)) {
          queryResDTO.setReviewUserUnit(reviewDeptDO.getName());
        }
      }
    }

    // 补充论文标题、杂志社和作者
    Map<Long, String> articleTitleMap = Maps.newHashMap();
    Map<Long, String> articleJournalMap = Maps.newHashMap();
    Map<Long, List<String>> authorNameMap = Maps.newHashMap();
    List<String> fileUrlList = Lists.newArrayList();
    List<String> imageList = Lists.newArrayList();

    List<ArticleDO> articleDOList = articleService.queryListByTaskId(queryResDTO.getId());
    for (ArticleDO articleDO : articleDOList) {
      if (queryResDTO.getFileType().equals("pdf")) {
        articleTitleMap.put(articleDO.getId(), articleDO.getArticleTitle());
        articleJournalMap.put(articleDO.getId(), articleDO.getArticleJournal());
        authorNameMap.put(articleDO.getId(), articleDO.getAuthorName());
        fileUrlList.add(articleDO.getFilePath());
      } else {
        imageList.add(articleDO.getFilePath());
      }
    }

    queryResDTO.setFileUrlList(fileUrlList);
    queryResDTO.setFirstImage(imageList);
    queryResDTO.setArticleTitleMap(articleTitleMap);
    queryResDTO.setArticleJournalMap(articleJournalMap);
    queryResDTO.setAuthorNameMap(authorNameMap);

    return CommonResult.success(queryResDTO);
  }

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
//      if (roleDo.getCode().equalsIgnoreCase("super_admin") || roleDo.getCode().equalsIgnoreCase("Research_admin")){
        AdminUserDO createUser = adminUserService.getUser(queryResDTO.getCreatorId());
        if (Objects.nonNull(createUser)) {
          queryResDTO.setUserName(createUser.getNickname());
          DeptDO deptDO = deptService.getDept(createUser.getDeptId());
          if (Objects.nonNull(deptDO)) {
            queryResDTO.setUserUnit(deptDO.getName());
          }
        }
//      }


      // 补充审核用户信息
      if (queryResDTO.getReviewerId() != null){
         AdminUserVO reviewUser = userInfoService.getUserById(queryResDTO.getReviewerId());
        if (Objects.nonNull(reviewUser)) {
          queryResDTO.setReviewUserName(reviewUser.getUsername());
          DeptDO reviewDeptDO = deptService.getDept(reviewUser.getDeptId());
          if (Objects.nonNull(reviewDeptDO)) {
            queryResDTO.setReviewUserUnit(reviewDeptDO.getName());
          }
        }
      }

      // 补充论文标题、杂志社和作者
      Map<Long, String> articleTitleMap = Maps.newHashMap();
      Map<Long, String> articleJournalMap = Maps.newHashMap();
      Map<Long, List<String>> authorNameMap = Maps.newHashMap();
      List<String> fileUrlList = Lists.newArrayList();
      List<String> imageList = Lists.newArrayList();

      List<ArticleDO> articleDOList = articleService.queryListByTaskId(queryResDTO.getId());
      for (ArticleDO articleDO : articleDOList) {
        if (queryResDTO.getFileType().equals("pdf")){
          articleTitleMap.put(articleDO.getId(), articleDO.getArticleTitle());
          articleJournalMap.put(articleDO.getId(), articleDO.getArticleJournal());
          authorNameMap.put(articleDO.getId(), articleDO.getAuthorName());
          fileUrlList.add(articleDO.getFilePath());
        } else {
          imageList.add(articleDO.getFilePath());
        }
      }

      queryResDTO.setFileUrlList(fileUrlList);
      queryResDTO.setFirstImage(imageList);
      queryResDTO.setArticleTitleMap(articleTitleMap);
      queryResDTO.setArticleJournalMap(articleJournalMap);
      queryResDTO.setAuthorNameMap(authorNameMap);
    }
    return pageResult;
  }



  //@Transactional(rollbackFor = Exception.class)
  public ImageTaskCreateResDTO createTask(ImageTaskCreateReqVO reqVO){

    // 参数检测
    log.info("createTask【1/8】start parse strategy param");
    if (reqVO.getTaskType() == 2) {
      if (Objects.isNull(reqVO.getTaskStrategyConfig()) ||
          (Objects.isNull(reqVO.getTaskStrategyConfig().getStartTime()) && Objects.isNull(reqVO.getTaskStrategyConfig().getEndTime())
          && CollectionUtils.isAnyEmpty(reqVO.getTaskStrategyConfig().getKeywordList())
          && Objects.isNull(reqVO.getTaskStrategyConfig().getMedicalSpecialty()))) {
        throw new RuntimeException("策略查，必须传策略参数");
      }
    }
    log.info("createTask【1/8】end parse strategy param");

    // 创建任务
    log.info("createTask【2/8】start insert task");
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
    log.info("createTask【2/8】end insert task, taskId={}, taskType={}", imageTaskDO.getId(),reqVO.getTaskType());


    // 任务ID
    log.info("createTask【3/8】start get filePath");
    String filePath = String.format(FilePathConstant.UPLOAD_PATH, imageTaskDO.getId());
    log.info("createTask【3/8】end get filePath, filePath={}", filePath);

    // 上传文件
    log.info("createTask【4/8】start upload file");
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isAnyEmpty(imageTaskResDTO.getSuccessFile())) {
      return imageTaskResDTO;
    }
    log.info("createTask【4/8】end upload file");

    // 更新任务
    log.info("createTask【5/8】start update task");
    List<FileContent> fileList = imageTaskResDTO.getSuccessFile();
    ImageTaskDO updateTask = new ImageTaskDO();
    updateTask.setId(imageTaskDO.getId());
    updateTask.setTotalImages(fileList.size());
    updateTask.setTaskNo("RW"+ LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN)
        + imageTaskDO.getId());
    imageTaskService.update(updateTask);
    log.info("createTask【5/8】end update task, taskId={}, fileSize={}", imageTaskDO.getId(),fileList.size());

    // 创建文件并异步解析PDF
    log.info("createTask【6/8】start parse pdf and insert article");
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
    log.info("createTask【6/8】end parse pdf and insert article, taskId={}", imageTaskDO.getId());

    // 异步算法检测
    log.info("createTask【7/8】start commit async");
    imageProcessService.processAsync(imageTaskDO.getId());
    log.info("createTask【7/8】end commit async");

    // 更新任务状态为算法检测中
    log.info("createTask【8/8】start update task, taskId={}", imageTaskDO.getId());
    ImageTaskDO updateImageTaskStatus = new ImageTaskDO();
    updateImageTaskStatus.setId(imageTaskDO.getId());
    updateImageTaskStatus.setTaskStatus(TaskStatusEnum.ALGO_DETECT.getCode());
    imageTaskService.update(updateImageTaskStatus);
    log.info("createTask【8/8】end update task, taskId={}", imageTaskDO.getId());

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

  // 删除专家用户绑定
  public CommonResult<String> clearTaskAllocation(Long id) {
    if (Objects.isNull(id)) {
      return CommonResult.error(500, "任务id不能为空");
    }
    ImageTaskDO imageTaskDO = imageTaskService.getById(id);
    if (Objects.isNull(imageTaskDO)) {
      return CommonResult.error(500, "任务不存在【" + id + "】");
    }
    
    // 清空专家分配信息
    Integer sum = imageTaskService.clearExpertUser(id);
    if (Objects.isNull(sum) || sum < 1){
      return CommonResult.error(500, "清空任务分配失败，请联系管理员");
    }
    return CommonResult.success("success");
  }

  public CommonResult<String> updateTask(ImageTaskUpdateReqVO updateReqVO) {
    Long id = updateReqVO.getId();
    if (Objects.isNull(id)) {
      return CommonResult.error(500, "任务id不能为空");
    }
    ImageTaskDO imageTaskDO = imageTaskService.getById(id);
    if (Objects.isNull(imageTaskDO)) {
      return CommonResult.error(500, "任务不存在【" + id + "】");
    }
    
    // 只有审核中的任务才允许修改
    if (!Objects.equals(imageTaskDO.getTaskStatus(), TaskStatusEnum.EXPERT_REVIEW.getCode())) {
      return CommonResult.error(500, "只有审核中的任务可以修改");
    }
    
    // 更新任务基本信息
    ImageTaskDO updateTask = new ImageTaskDO();
    updateTask.setId(id);
    updateTask.setTaskType(updateReqVO.getTaskType());
    updateTask.setReviewResult(updateReqVO.getReviewResult());
    
    Integer sum = imageTaskService.update(updateTask);
    if (Objects.isNull(sum) || sum < 1){
      return CommonResult.error(500, "更新任务失败，请联系管理员");
    }
    
    // 更新文章信息
    if (!CollectionUtils.isAnyEmpty(updateReqVO.getArticleTitleList(), updateReqVO.getArticleJournalList())) {
      
      List<ArticleDO> articleDOList = articleService.queryListByTaskId(id);
      if (!CollectionUtils.isAnyEmpty(articleDOList)) {
        // 更新文章标题和杂志名
        for (int i = 0; i < articleDOList.size(); i++) {
          ArticleDO articleDO = articleDOList.get(i);
          
          if (updateReqVO.getArticleTitleList() != null && i < updateReqVO.getArticleTitleList().size()) {
            articleDO.setArticleTitle(updateReqVO.getArticleTitleList().get(i));
          }
          
          if (updateReqVO.getArticleJournalList() != null && i < updateReqVO.getArticleJournalList().size()) {
            articleDO.setArticleJournal(updateReqVO.getArticleJournalList().get(i));
          }
          
          articleService.update(articleDO);
        }
      }
    }
    
    return CommonResult.success("success");
  }


}
