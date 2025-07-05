package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.task.ImageTaskApiService;
import cn.iocoder.yudao.module.system.api.task.ImgSimilarApiService;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskCreateResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.ImageTaskQueryResDTO;
import cn.iocoder.yudao.module.system.api.task.dto.TaskStrategyConfig;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarCompareResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.similar.ImgSimilarityReviewReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskAllocateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskCreateResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.task.ImageTaskReviewReqVO;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

@Tag(name = "管理后台 - 任务管理-相似图片比对")
@RestController
@RequestMapping("/task/similar")
@Slf4j
public class SimilarController {

  @Resource
  private ImgSimilarApiService imgSimilarApiService;


  @GetMapping("/query")
  public CommonResult<PageResult<ImgSimilarQueryResVO>> pageQuery(ImgSimilarityQueryReqVO reqVO) {
    try {
      PageResult<ImgSimilarQueryResVO> pageResult =  imgSimilarApiService.query(reqVO);
      return CommonResult.success(pageResult);
    }catch (Exception e) {
      log.error("查询失败，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }


  @PostMapping("/review")
  public CommonResult<String> review(@RequestBody ImgSimilarityReviewReqVO reviewReqVO) {
    try {
      return imgSimilarApiService.reviewSimilar(reviewReqVO);
    }catch (Exception e) {
      log.error("review error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

  @PostMapping("/compare/{id}")
  public CommonResult<ImgSimilarCompareResVO> compare(@PathVariable Long id) {
    try {
      return imgSimilarApiService.compare(id);
    }catch (Exception e) {
      log.error("compare error，",e);
      return CommonResult.error(new ErrorCode(500, e.getMessage()));
    }
  }

}
