package cn.iocoder.yudao.module.system.api.task;


import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.task.dto.SmallImageMilvusDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileUpdateReqVO;
import cn.iocoder.yudao.module.system.api.task.common.DbImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.FileUploadService;
import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryResVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.dal.mysql.task.ArticleMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.task.SmallImageDO;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import com.alibaba.druid.util.StringUtils;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ArticleApiService {

  private static final String UPLOAD_PATH = "./task-file/%s";

  @Resource
  private ArticleService articleService;

  @Resource
  private FileUploadService fileUploadService;

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private DbImageProcessService dbImageProcessService;

  @Resource
  private ArticleMapper articleMapper;
  @Resource
  private MilvusOperateService milvusOperateService;

  public CommonResult<PageResult<FileQueryResVO>> pageQuery(FileQueryReqVO fileQueryReqVO) {

    if (StringUtils.equals(fileQueryReqVO.getFileType(),"image")){
      fileQueryReqVO.setIsImage(Boolean.TRUE);
    }else if (StringUtils.equals(fileQueryReqVO.getFileType(),"pdf")){
      fileQueryReqVO.setIsImage(Boolean.FALSE);
    }

    PageResult<ArticleDO> pageResult = articleService.queryPage(fileQueryReqVO);
    PageResult<FileQueryResVO> finalResult = BeanUtils.toBean(pageResult, FileQueryResVO.class);
    for (FileQueryResVO fileQueryResVO : finalResult.getList()) {

      Long largeImageSum = largeImageService.querySumByArticleId(fileQueryResVO.getId());
      fileQueryResVO.setLargeImageSum(largeImageSum);

      Long smallImageSum = smallImageService.querySumByArticleId(fileQueryResVO.getId());
      fileQueryResVO.setSmallImageSum(smallImageSum);
    }
    return CommonResult.success(finalResult);
  }

  public CommonResult<Integer> batchDelete(List<Long> ids) {
    if (CollectionUtils.isEmpty(ids)){
      return CommonResult.error(500, "请先选择文章");
    }
    for (long id : ids) {
      CommonResult<Integer> result = deleteById(id);
      if (!result.isSuccess()){
        log.error("delete article id [{}] fail", id);
      }
    }
    return CommonResult.success(ids.size());
  }


  @Transactional(rollbackFor = Exception.class)
  public CommonResult<Integer> deleteById(Long id) {

    log.info("deleteArticleById【1/4】start delete milvus, articleId = {}", id);
    List<SmallImageDO> smallImageDOList = smallImageService.queryByArticleId(id);
    List<Long> smallImageIds = smallImageDOList.stream().map(SmallImageDO::getId).collect(Collectors.toList());
    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(smallImageIds)) {
      for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
        milvusOperateService.deleteByPrimaryId(smallImageIds, modelNameEnum.getCollectionName());
      }
    } else {
      log.info("deleteArticleById【1/4】skip milvus delete because smallImageIds is empty, articleId = {}", id);
    }
    log.info("deleteArticleById【1/4】end delete milvus, articleId = {}, smallSize", id);

    log.info("deleteArticleById【2/4】start delete article, id = {}", id);
    if (id == null) {
      return CommonResult.error(500, "请先选择文章");
    }
    Integer count = articleService.deleteById(id);
    if (count == null || count <= 0) {
      throw new RuntimeException("删除文章失败：" + id);
    }
    log.info("deleteArticleById【2/4】end delete article, id = {}", id);

    log.info("deleteArticleById【3/4】start delete large image, articleId = {}", id);
    count = largeImageService.deleteByArticleId(id);
    log.info("deleteArticleById【3/4】end delete large image, articleId = {}, count = {}", id,count);

    log.info("deleteArticleById【4/4】start delete small image, articleId = {}", id);
    count = smallImageService.deleteByArticleId(id);
    log.info("deleteArticleById【4/4】end delete small image, articleId = {}, count = {}", id,count);

    return CommonResult.success(count);
  }

  @Transactional(rollbackFor = Exception.class)
  public CommonResult<String> create(FileCreateReqVO reqVO){

    // 获取文件上传地址
    log.info("createArticle【1/4】start get file path, fileType = {}", reqVO.getFileType());
    String filePath = String.format(UPLOAD_PATH, UUID.randomUUID());
    log.info("createArticle【1/4】end get file path, fileType = {}", reqVO.getFileType());

    // 上传文件
    log.info("createArticle【2/4】start upload file");
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isEmpty(imageTaskResDTO.getSuccessFile())) {
      return CommonResult.error(500, "文件上传失败");
    }
    log.info("createArticle【2/4】end upload file");

    // 创建文件并异步解析PDF
    log.info("createArticle【3/4】start parse pdf");
    List<FileContent> fileList = imageTaskResDTO.getSuccessFile();
    List<String> filePathList = fileList.stream().map(FileContent::getFilePath).collect(Collectors.toList());
    log.info("createArticle【3/4】end parse pdf");

    // 向量化
    log.info("createArticle【4/4】start vector");
    dbImageProcessService.processFileBatchAsync(filePathList, reqVO.getFileType());
    log.info("createArticle【4/4】end vector");

    return CommonResult.success("success");
  }


  public void updateFilesInBatch(FileUpdateReqVO updateReqVO) {

    List<ArticleDO> articleDOList = Lists.newArrayList();
    for (FileUpdateReqVO.FileUpdateItem item : updateReqVO.getFiles()) {
      // 创建一个 ArticleDO 对象用于更新
      ArticleDO articleUpdate = new ArticleDO();
      articleUpdate.setId(item.getId());
      articleUpdate.setArticleTitle(item.getArticleTitle());
      articleUpdate.setArticleJournal(item.getArticleJournal());
      articleUpdate.setAuthorName(item.getAuthorName());
      articleUpdate.setAuthorInstitution(item.getAuthorInstitution());
      articleDOList.add(articleUpdate);
    }

    // 调用 Mapper 更新数据库中的记录
    Boolean flag = articleMapper.updateBatch(articleDOList);
    log.info("批量更新文章信息, flag={}, size = {}", flag, articleDOList.size());

    // 开始更新Milvus
    int batch = 50;
    List<SmallImageMilvusDTO> smallImageMilvusDTOList = Lists.newArrayList();
    for (ArticleDO articleDO : articleDOList) {
      List<SmallImageDO> smallImageDOList = smallImageService.queryByArticleId(articleDO.getId());
      ArticleDO newArticle = articleService.batchQueryById(articleDO.getId());
      for (SmallImageDO smallImageDO : smallImageDOList) {
        SmallImageMilvusDTO imageMilvusDTO = new SmallImageMilvusDTO();
        imageMilvusDTO.setId(smallImageDO.getId());
        imageMilvusDTO.setArticleId(newArticle.getId());
        imageMilvusDTO.setKeywords(newArticle.getArticleKeywords());
        imageMilvusDTO.setAuthor(newArticle.getAuthorName());
        imageMilvusDTO.setInstitution(newArticle.getAuthorInstitution());
        imageMilvusDTO.setArticleDate(newArticle.getArticleDate());
        imageMilvusDTO.setSpecialty(newArticle.getMedicalSpecialty());
        smallImageMilvusDTOList.add(imageMilvusDTO);
        if (smallImageMilvusDTOList.size() >= batch){
          milvusOperateService.updateByPrimaryId(smallImageMilvusDTOList);
        }
      }
    }
    if (CollectionUtils.isNotEmpty(smallImageMilvusDTOList)){
      milvusOperateService.updateByPrimaryId(smallImageMilvusDTOList);
    }
  }

  /**
   * 获取所有文章的单位列表
   */
  public CommonResult<List<String>> queryAllUniList() {
    return CommonResult.success(articleMapper.getAllUnitDistributionList());
  }
}
