package cn.iocoder.yudao.module.system.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 单位分布数据 Response VO")
@Data
public class StatisticsUnitDistributionRespVO {

    @Schema(description = "单位/专家名称")
    private String name;

    @Schema(description = "数量")
    private Long value;

    @Schema(description = "百分比")
    private Double percentage;
} 