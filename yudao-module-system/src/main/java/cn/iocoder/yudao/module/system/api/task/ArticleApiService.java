package cn.iocoder.yudao.module.system.api.task;


import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.task.common.FileUploadService;
import cn.iocoder.yudao.module.system.api.task.common.ImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.PdfArticleParseService;
import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryResVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
  private PdfArticleParseService pdfArticleParseService;

  @Resource
  private FileUploadService fileUploadService;

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private ImageProcessService imageProcessService;

  public CommonResult<PageResult<FileQueryResVO>> pageQuery(FileQueryReqVO fileQueryReqVO) {
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
    if (CollectionUtils.isAnyEmpty(ids)){
      return CommonResult.error(500, "请先选择文章");
    }
    Integer sum = articleService.batchDelete(ids);
    return CommonResult.success(sum);
  }

  public CommonResult<Integer> deleteById(Long id) {
    if (id == null) {
      return CommonResult.error(500, "请先选择文章");
    }
    Integer count = articleService.deleteById(id);
    return CommonResult.success(count);
  }

  @Transactional(rollbackFor = Exception.class)
  public CommonResult<String> create(FileCreateReqVO reqVO){

    // 任务ID
    String filePath = String.format(UPLOAD_PATH, UUID.randomUUID().toString());

    // 上传文件
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isAnyEmpty(imageTaskResDTO.getSuccessFile())) {
      return CommonResult.error(500, "文件上传失败");
    }

    // 创建文件并异步解析PDF
    List<FileContent> fileList = imageTaskResDTO.getSuccessFile();
    List<ArticleDO> articleDOList = Lists.newArrayList();
    for (FileContent fileContent : fileList) {
      ArticleDO articleDO = new ArticleDO();
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
      throw new RuntimeException("文件入库失败");
    }

    // 对PDF文件进行异步解析
    if ("pdf".equalsIgnoreCase(reqVO.getFileType())) {
      for (ArticleDO articleDO : articleDOList) {
        pdfArticleParseService.asyncParsePdfAndUpdate(articleDO);
      }
    }

    // todo 异步调用算法切割图片&提取向量特征


    return CommonResult.success("success");
  }


}
