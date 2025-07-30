package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 完成图片任务审核 Request VO")
@Data
public class ImageTaskCompleteReviewReqVO {

    @Schema(description = "任务编号", required = true, example = "1024")
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "审核结果(2检测无异常,3检测有异常)", required = true, example = "2")
    @NotNull(message = "审核结果不能为空")
    private Integer reviewResult;

} 