package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.task.LargeImageApiService;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.LargeImageQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.LargeImageQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.image.LargeImageUpdateReqVO;
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

@Tag(name = "管理后台 - 任务管理 -- 大图管理")
@RestController
@RequestMapping("/largeImage/manager")
@Slf4j
public class LargeImageController {

  @Resource
  private LargeImageApiService largeImageApiService;

  @GetMapping("/query")
  public CommonResult<PageResult<LargeImageQueryResVO>> create(LargeImageQueryReqVO reqVO) {
    try {
      return largeImageApiService.pageQuery(reqVO);
    }catch (Exception e) {
      log.error("查询失败，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/deleteById/{id}")
  public CommonResult<Integer> deleteById(@PathVariable Long id) {
    try {
      return largeImageApiService.deleteById(id);
    }catch (Exception e) {
      log.error("deleteById error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/deleteByIds")
  public CommonResult<Integer> deleteById(@RequestBody List<Long> ids) {
    try {
      return largeImageApiService.deleteByIds(ids);
    }catch (Exception e) {
      log.error("deleteByIds error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }


  @PostMapping("/update")
  public CommonResult<Integer> update(@RequestBody LargeImageUpdateReqVO reqVO) {
    try {
      return largeImageApiService.update(reqVO);
    }catch (Exception e) {
      log.error("deleteByIds error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

}
