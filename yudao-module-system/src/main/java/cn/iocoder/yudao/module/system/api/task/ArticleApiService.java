package cn.iocoder.yudao.module.system.api.task;


import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.task.common.DbImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.FileUploadService;
import cn.iocoder.yudao.module.system.api.task.common.TaskImageProcessService;
import cn.iocoder.yudao.module.system.api.task.common.PdfArticleParseService;
import cn.iocoder.yudao.module.system.api.task.dto.FileContent;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryResVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ArticleDO;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.LargeImageService;
import cn.iocoder.yudao.module.system.service.task.SmallImageService;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private FileUploadService fileUploadService;

  @Resource
  private LargeImageService largeImageService;

  @Resource
  private SmallImageService smallImageService;

  @Resource
  private DbImageProcessService dbImageProcessService;

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
    String filePath = String.format(UPLOAD_PATH, UUID.randomUUID());

    // 上传文件
    MultipartFile[] files = reqVO.getFiles();
    ImageTaskCreateResDTO imageTaskResDTO = fileUploadService.uploadFiles(files, filePath);
    if (!Boolean.TRUE.equals(imageTaskResDTO.getSuccess()) || CollectionUtils.isAnyEmpty(imageTaskResDTO.getSuccessFile())) {
      return CommonResult.error(500, "文件上传失败");
    }

    // 创建文件并异步解析PDF
    List<FileContent> fileList = imageTaskResDTO.getSuccessFile();
    List<String> filePathList = fileList.stream().map(FileContent::getFilePath).collect(Collectors.toList());

    // todo 五个库
    String collectionName = ModelNameEnum.ResNet50.getCode()+"_vectors";
    dbImageProcessService.processFileBatchAsync(filePathList, reqVO.getFileType(),collectionName);

    return CommonResult.success("success");
  }


}
