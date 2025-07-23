package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.task.common.DbImageProcessService;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.ManualProcessFileVO;
import cn.iocoder.yudao.module.system.enums.task.ModelNameEnum;
import cn.iocoder.yudao.module.system.service.task.ArticleService;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 任务管理-手动执行脚本")
@RestController
@RequestMapping("/manual/script")
@Slf4j
public class ManualScriptController {

  @Resource
  private MilvusOperateService milvusOperateService;

  @Resource
  private DbImageProcessService dbImageProcessService;

  @Resource
  private ArticleService articleService;

  @GetMapping("/dump/milvus/{alias}/{length}")
  public CommonResult<String> dumpMilvus(@PathVariable String alias) {
    try {
      milvusOperateService.fullDump(alias);
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("dumpMilvus error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/dump/all/milvus")
  public CommonResult<String> dumpAllMilvus() {
    try {
      for (ModelNameEnum modelNameEnum : ModelNameEnum.values()) {
        milvusOperateService.fullDump(modelNameEnum.getCollectionName());
      }
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("dumpMilvus error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/write/data/milvus/{alias}")
  public CommonResult<String> writeDataMilvus(@PathVariable String alias) {
    try {
      // 获取算法
      ModelNameEnum modelNameEnum = ModelNameEnum.ResNet50;
      for (ModelNameEnum modelNameEnum1 : ModelNameEnum.values()) {
        if (modelNameEnum1.getCode().equals(alias)){
          modelNameEnum = modelNameEnum1;
        }
      }
      milvusOperateService.batchWriteDataFromDb(modelNameEnum.getCollectionName(), modelNameEnum);
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("dumpMilvus error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/only/processFile/{path}/{type}")
  public CommonResult<String> onlyProcessFile(@PathVariable String path, @PathVariable String type) {
    try {
      dbImageProcessService.batchHandleFileParentDirectory(path,type);
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("processFile error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/repeat/processFile")
  public CommonResult<String> repeatProcessFile(@RequestBody List<Long> articleIdList) {
    try {
      dbImageProcessService.batchRepeatHandleImage(articleIdList);
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("repeat processFile error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/only/processFileList")
  public CommonResult<String> onlyProcessFile(@RequestBody ManualProcessFileVO manualProcessFileVO) {
    try {
      dbImageProcessService.batchHandleFileList(manualProcessFileVO.getFileList(),manualProcessFileVO.getFileType());
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("processFile error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/repeat/handleAllFile")
  public CommonResult<String> handleAllFile() {
    try {
      TenantContextHolder.setTenantId(1L);
      Long maxId = articleService.maxId();
      Long minId = articleService.minId();
      for (Long articleId = minId; articleId <= maxId; articleId++) {
        dbImageProcessService.repeatProcessFileSingle(articleId);
      }
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("repeat processFile error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @GetMapping("/repeat/handleDbFile")
  public CommonResult<String> handleDbFile() {
    try {
      TenantContextHolder.setTenantId(1L);
      Long maxId = 377L;
      Long minId = 355L;
      for (Long articleId = minId; articleId <= maxId; articleId++) {
        dbImageProcessService.repeatProcessFileSingle(articleId);
      }
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("repeat processFile error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

}
