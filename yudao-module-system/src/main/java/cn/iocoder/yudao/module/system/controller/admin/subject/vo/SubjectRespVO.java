package cn.iocoder.yudao.module.system.controller.admin.subject.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 学科 Response VO")
@Data
public class SubjectRespVO {

    @Schema(description = "学科编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "学科名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "计算机科学与技术")
    private String name;

    @Schema(description = "父级编号", example = "0")
    private Long parentId;

    @Schema(description = "层级", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer level;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子学科列表")
    private List<SubjectRespVO> children;

} 