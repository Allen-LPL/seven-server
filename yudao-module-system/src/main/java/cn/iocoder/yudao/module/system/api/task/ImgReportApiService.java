package cn.iocoder.yudao.module.system.api.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ImgReportQueryReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ImgReportQueryResVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.service.task.ImgReportService;
import cn.iocoder.yudao.module.system.service.task.ImgSimilarityService;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImgReportApiService {

  @Resource
  private ImgReportService imgReportService;

//  public CommonResult<PageResult<ImgReportDO>> pageQuery(ReportPageReqVO reqVO){
//    PageResult<ImgReportDO> imageDOPageResult =  imgReportService.pageResult(reqVO);
////    PageResult<ImgReportQueryResVO> pageResult = BeanUtils.toBean(imageDOPageResult,ImgReportQueryResVO.class);
//    return CommonResult.success(imageDOPageResult);
//  }

  public CommonResult<Integer> deleteByIds(List<Long> ids){
    if (CollectionUtils.isAnyEmpty(ids)){
      return CommonResult.error(500, "请选择图片");
    }
    Integer sum = imgReportService.deleteByIds(ids);
    return CommonResult.success(sum);
  }

  public CommonResult<Integer> deleteById(Long id){
    if (Objects.isNull(id)){
      return CommonResult.error(500, "请选择图片");
    }
    Integer sum = imgReportService.deleteById(id);
    return CommonResult.success(sum);
  }

}
