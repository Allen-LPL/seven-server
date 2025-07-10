package cn.iocoder.yudao.module.system.controller.admin.statistics;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsDataRespVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsRangeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsTrendRespVO;
import cn.iocoder.yudao.module.system.controller.admin.statistics.vo.StatisticsUnitDistributionRespVO;
import cn.iocoder.yudao.module.system.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 系统统计")
@RestController
@RequestMapping("/system/statistics")
@Validated
@Slf4j
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取统计概览数据")
    @PermitAll
    public CommonResult<StatisticsDataRespVO> getOverviewData() {
        return success(statisticsService.getOverviewData());
    }

    @GetMapping("/trend")
    @Operation(summary = "获取趋势分析数据")
    @PermitAll
    public CommonResult<StatisticsTrendRespVO> getTrendData(StatisticsRangeReqVO reqVO) {
        return success(statisticsService.getTrendData(reqVO));
    }

    @GetMapping("/unit-distribution")
    @Operation(summary = "获取异常单位分布数据")
    @PreAuthorize("@ss.hasPermission('system:admin-query')")
    public CommonResult<List<StatisticsUnitDistributionRespVO>> getUnitDistributionData() {
        return success(statisticsService.getUnitDistributionData(WebFrameworkUtils.getLoginUserId()));
    }

    @GetMapping("/report-audit")
    @Operation(summary = "获取报告审核统计数据")
    @PermitAll
    public CommonResult<List<StatisticsUnitDistributionRespVO>> getReportAuditData() {
        return success(statisticsService.getReportAuditData());
    }
}
