package cn.iocoder.yudao.module.system.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 统计概览数据 Response VO")
@Data
public class StatisticsDataRespVO {

    @Schema(description = "论文总数", example = "15680")
    private Long totalArticles;

    @Schema(description = "本期新增论文数", example = "1200")
    private Long newArticles;

    @Schema(description = "图片总数", example = "68742")
    private Long totalImages;

    @Schema(description = "本期新增图片数", example = "5320")
    private Long newImages;

    @Schema(description = "专家总数", example = "158")
    private Long totalExperts;

    @Schema(description = "本期新增专家数", example = "12")
    private Long newExperts;

    @Schema(description = "用户总数", example = "2450")
    private Long totalUsers;

    @Schema(description = "本期新增用户数", example = "180")
    private Long newUsers;

    @Schema(description = "单位总数", example = "326")
    private Long totalUnits;

    @Schema(description = "本期新增单位数", example = "18")
    private Long newUnits;

    @Schema(description = "累计异常数", example = "1245")
    private Long totalAbnormal;

    @Schema(description = "本期新增异常数", example = "89")
    private Long newAbnormal;
} 