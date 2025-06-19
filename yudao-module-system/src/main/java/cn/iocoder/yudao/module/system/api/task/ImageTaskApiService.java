package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.io.FileUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskQueryResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImageTaskDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.ImageTaskService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


  private static final String UPLOAD_PATH = "./task/%s";

  public PageResult<ImageTaskQueryResDTO> query(ImageTaskQueryReqVO imageTaskQueryReqVO){
    PageResult<ImageTaskDO> imageTaskDOPageResult = imageTaskService.pageQuery(imageTaskQueryReqVO);
    PageResult<ImageTaskQueryResDTO> pageResult = BeanUtils.toBean(imageTaskDOPageResult, ImageTaskQueryResDTO.class);
    List<ImageTaskQueryResDTO> queryResDTOList = pageResult.getList();
    for (ImageTaskQueryResDTO queryResDTO : queryResDTOList) {
      // 补充创建用户信息
      AdminUserDO createUser = adminUserService.getUser(queryResDTO.getCreatorId());
      if (Objects.nonNull(createUser)) {
        queryResDTO.setUserName(createUser.getUsername());
      }
      DeptDO deptDO = deptService.getDept(createUser.getDeptId());
      if (Objects.nonNull(deptDO)) {
        queryResDTO.setUserUnit(deptDO.getName());
      }

      // 补充审核用户信息
      AdminUserDO reviewUser = adminUserService.getUser(queryResDTO.getReviewerId());
      if (Objects.nonNull(reviewUser)) {
        queryResDTO.setReviewUserName(reviewUser.getUsername());
      }
      DeptDO reviewDeptDO = deptService.getDept(createUser.getDeptId());
      if (Objects.nonNull(reviewDeptDO)) {
        queryResDTO.setReviewUserUnit(reviewDeptDO.getName());
      }

      // 补充论文标题和杂志社
      List<String> articleTitleList = Lists.newArrayList();
      List<String> articleJournalList = Lists.newArrayList();
      if (queryResDTO.getFileType().equals("pdf")){
        List<ArticleDO> articleDOList = articleService.queryListByTaskId(queryResDTO.getTaskId());
        for (ArticleDO articleDO : articleDOList) {
          articleTitleList.add(articleDO.getArticleTitle());
          articleJournalList.add(articleDO.getArticleJournal());
        }
      }
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

    // 任务ID
    String uuid = UUID.randomUUID().toString();
    String filePath = String.format(UPLOAD_PATH, uuid);

    // 上传文件
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isAnyEmpty(imageTaskResDTO.getSuccessFile())) {
      return imageTaskResDTO;
    }

    // 创建任务
    ImageTaskDO imageTaskDO = new ImageTaskDO();
    imageTaskDO.setCreatorId(WebFrameworkUtils.getLoginUserId());
    imageTaskDO.setFirstImage(imageTaskResDTO.getSuccessFile().get(0));
    imageTaskDO.setTaskType(reqVO.getTaskType());
    if (reqVO.getTaskType() == 2) {
      imageTaskDO.setStrategyConfig(JSONObject.toJSONString(reqVO.getTaskStrategyConfig()));
    }
    Integer sum = imageTaskService.create(imageTaskDO);
    if (Objects.isNull(sum) || sum < 1){
      throw new RuntimeException("任务入库失败");
    }

    // 创建文件
    List<String> fileList = imageTaskResDTO.getSuccessFile();
    List<ArticleDO> articleDOList = Lists.newArrayList();
    for (String fullFilePath : fileList) {
      String fileName = fullFilePath.substring(fullFilePath.lastIndexOf("."));
      ArticleDO articleDO = new ArticleDO();
      articleDO.setTaskId(imageTaskDO.getTaskId());
      articleDO.setFileName(fileName);
      articleDO.setFilePath(fullFilePath);
      articleDO.setFileSize(0L);
      articleDO.setFileType(reqVO.getFileType());

      // todo :需要提取
      articleDO.setArticleDate(System.currentTimeMillis());
      articleDO.setArticleJournal("aaa");
      articleDO.setArticleTitle("aaa");
      if ("pdf".equalsIgnoreCase(reqVO.getFileType())){
        articleDO.setIsImage(0);
      }else if ("image".equalsIgnoreCase(reqVO.getFileType())){
        articleDO.setIsImage(1);
      }
      articleDO.setArticleKeywords(JSONObject.toJSONString(Lists.newArrayList("aa","bb")));
      articleDO.setPmid("pmid");
      articleDO.setAuthorInstitution("cccc");
      articleDO.setAuthorName("aaaa");
      articleDO.setMedicalSpecialty("medical specialty");
      articleDOList.add(articleDO);
    }
    Boolean success = articleService.batchCreate(articleDOList);
    if (!success){
      throw new RuntimeException("任务入库失败");
    }

    // todo 异步调用算法接口  算法检测

    return imageTaskResDTO;
  }

  private ImageTaskCreateResDTO uploadFiles(MultipartFile[] files, String taskFilePath){

    ImageTaskCreateResDTO imageTaskResDTO = new ImageTaskCreateResDTO();
    try {
      // 确保上传目录存在
      File uploadDir = new File(UPLOAD_PATH);
      if (!uploadDir.exists()) {
        uploadDir.mkdirs();
      }

      List<String> failedFile = new ArrayList<>();
      List<String> successFile = new ArrayList<>();

      // 上传文件
      for (MultipartFile file : files) {
        String originalFilename = file.getOriginalFilename();

        try {
          // 验证文件
//          if (!isValidFileType(file)) {
//            failedFile.add(originalFilename + " (无效文件类型)");
//            imageTaskResDTO.setSuccess(Boolean.FALSE);
//            continue;
//          }

          if (!isValidFileSize(file)) {
            failedFile.add(originalFilename + " (文件过大)");
            imageTaskResDTO.setSuccess(Boolean.FALSE);
            continue;
          }

          // 文件名称
          String fileName = originalFilename.substring(originalFilename.lastIndexOf("."));

          // 保存文件
          String fullLocalFilePath = taskFilePath + fileName;
          Path filePath = Paths.get(fullLocalFilePath);
          Files.write(filePath, file.getBytes());

          successFile.add(fullLocalFilePath);
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

}
