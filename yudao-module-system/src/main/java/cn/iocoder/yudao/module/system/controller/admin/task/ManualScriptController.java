package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.service.task.MilvusOperateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 任务管理-手动执行脚本")
@RestController
@RequestMapping("/manual/script")
@Slf4j
public class ManualScriptController {

  @Resource
  private MilvusOperateService milvusOperateService;

  @GetMapping("/dump/milvus")
  public CommonResult<String> dumpMilvus(ImgSimilarityQueryReqVO reqVO) {
    try {
      String alias = "";
      int length = 2048;
      milvusOperateService.fullDump(alias,length);
      return CommonResult.success("success");
    }catch (Exception e) {
      log.error("dumpMilvus error: ，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

}
