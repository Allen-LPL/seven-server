package cn.iocoder.yudao.module.system.controller.admin.task.vo.report;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 生成报告 Request VO")
@Data
public class ReportGenerateReqVO {

  @Schema(description = "任务ID", required = true, example = "1024")
  @NotNull(message = "任务ID不能为空")
  private Long taskId;

  @Schema(description = "报告名称", required = true, example = "报告1")
  @NotNull(message = "报告名称不能为空")
  private Long name;
}
