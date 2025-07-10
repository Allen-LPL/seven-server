package cn.iocoder.yudao.module.system.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 趋势分析数据 Response VO")
@Data
public class StatisticsTrendRespVO {

    @Schema(description = "异常趋势数据")
    private TrendData abnormalTrends;

    @Schema(description = "异常领域分布数据")
    private FieldDistributionData fieldDistribution;

    @Schema(description = "单日最高异常数")
    private Long maxAnomalyCount;

    @Schema(description = "单日最低异常数")
    private Long minAnomalyCount;

    @Schema(description = "日均异常数")
    private Long avgAnomalyCount;

    @Schema(description = "总异常数")
    private Long totalAnomalyCount;

    @Schema(description = "分析天数")
    private Integer daysCount;

    @Schema(description = "趋势数据")
    @Data
    public static class TrendData {
        @Schema(description = "日期列表")
        private List<String> dates;

        @Schema(description = "数据列表")
        private List<Long> values;
    }

    @Schema(description = "领域分布数据")
    @Data
    public static class FieldDistributionData {
        @Schema(description = "领域名称列表")
        private List<String> fields;

        @Schema(description = "数据列表")
        private List<Long> values;
    }
} 