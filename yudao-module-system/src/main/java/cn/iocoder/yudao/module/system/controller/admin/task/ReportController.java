package cn.iocoder.yudao.module.system.controller.admin.task;


import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportGenerateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.task.vo.report.ReportPageRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.task.ImgReportDO;
import cn.iocoder.yudao.module.system.service.task.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 报告")
@RestController
@RequestMapping("/report/manager")
@Slf4j
public class ReportController {

    @Resource
    private ReportService reportService;

  /**
   * 生成报告接口
   *
   * @param reqVO ReportGenerateReqVO
   * @param file MultipartFile
   * @return CommonResult<Long>
   */
  @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "生成报告")
//  @PreAuthorize("@ss.hasPermission('system:report:generate')")
  public CommonResult<Long> generateReport(@Valid ReportGenerateReqVO reqVO,
                                           @RequestParam("file") MultipartFile file) {
    return reportService.generateReport(reqVO, file);
  }


  @GetMapping("/get")
  @Operation(summary = "查询报告详情")
  @Parameter(name = "id", description = "报告编号", required = true, example = "1024")
  @PreAuthorize("@ss.hasPermission('system:report:query')")
  public CommonResult<ReportPageRespVO> getReport(@RequestParam("id") Long id) {
    ImgReportDO report = reportService.getReport(id);
    return success(BeanUtils.toBean(report, ReportPageRespVO.class));
  }

  @DeleteMapping("/delete")
  @Operation(summary = "删除报告")
  @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
  @PreAuthorize("@ss.hasPermission('system:report:delete')")
  public CommonResult<Boolean> deleteReport(@RequestParam("ids") List<Long> ids) {
    reportService.deleteReport(ids);
    return success(true);
  }


    @GetMapping("/page")
    @Operation(summary = "获得报告分页")
    @PreAuthorize("@ss.hasPermission('system:report:query')")
    public CommonResult<PageResult<ReportPageRespVO>> getReportPage(@Valid ReportPageReqVO pageVO) {
        PageResult<ReportPageRespVO> pageResult = reportService.getReportPage(pageVO);
        return success(pageResult);
    }
}
