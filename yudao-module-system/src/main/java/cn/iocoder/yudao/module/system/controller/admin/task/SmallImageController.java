package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.task.SmallImageApiService;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.SmallImageQueryResVO;
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

@Tag(name = "管理后台 - 任务管理")
@RestController
@RequestMapping("/smallImage/manager")
@Slf4j
public class SmallImageController {

  @Resource
  private SmallImageApiService smallImageApiService;

  @GetMapping("/query")
  public CommonResult<PageResult<SmallImageQueryResVO>> create(SmallImageQueryReqVO reqVO) {
    try {
      return smallImageApiService.pageQuery(reqVO);
    }catch (Exception e) {
      log.error("查询失败，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/deleteById/{id}")
  public CommonResult<Integer> deleteById(@PathVariable Long id) {
    try {
      return smallImageApiService.deleteById(id);
    }catch (Exception e) {
      log.error("deleteById error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/deleteByIds")
  public CommonResult<Integer> deleteById(@RequestBody List<Long> ids) {
    try {
      return smallImageApiService.deleteByIds(ids);
    }catch (Exception e) {
      log.error("deleteByIds error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }


}
