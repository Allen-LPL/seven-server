package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.task.ArticleApiService;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.file.FileQueryResVO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理后台 - 任务管理 -- 文章管理")
@RestController
@RequestMapping("/article/manager")
@Slf4j
public class ArticleController {

  @Resource
  private ArticleApiService articleApiService;

  @RequestMapping(method = RequestMethod.POST, value = "/create" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CommonResult<String> create( @Parameter(description = "上传文件", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) @RequestPart("files") MultipartFile[] files,
       @RequestParam("fileType") String fileType) {
    try {
      FileCreateReqVO reqVO = new FileCreateReqVO();
      reqVO.setFiles(files);
      reqVO.setFileType(fileType);
      return articleApiService.create(reqVO);
    }catch (Exception e) {
      log.error("创建检测任务失败, ",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/query")
  public CommonResult<PageResult<FileQueryResVO>> query(FileQueryReqVO reqVO) {
    try {
      return articleApiService.pageQuery(reqVO);
    }catch (Exception e) {
      log.error("查询失败，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/deleteById/{id}")
  public CommonResult<Integer> deleteById(@PathVariable Long id) {
    try {
      return articleApiService.deleteById(id);
    }catch (Exception e) {
      log.error("deleteById error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/deleteByIds")
  public CommonResult<Integer> deleteById(@RequestBody List<Long> ids) {
    try {
      return articleApiService.batchDelete(ids);
    }catch (Exception e) {
      log.error("deleteByIds error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }


}
