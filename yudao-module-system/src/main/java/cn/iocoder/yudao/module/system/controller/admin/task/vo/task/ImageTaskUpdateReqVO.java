package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 任务更新 Request VO")
@Data
public class ImageTaskUpdateReqVO {

    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Schema(description = "任务类型", example = "1")
    private Integer taskType;

    @Schema(description = "审核结果", example = "1")
    private Integer reviewResult;

    @Schema(description = "文章标题列表")
    private List<String> articleTitleList;

    @Schema(description = "杂志名列表") 
    private List<String> articleJournalList;

    @Schema(description = "是否案例（true=是，false=否）")
    private Boolean isCase;
} 