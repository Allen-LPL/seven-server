package cn.iocoder.yudao.module.system.controller.admin.task.vo.report;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 报告分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReportPageReqVO extends PageParam {

  @Schema(description = "报告ID", example = "123")
  private Long id;

  @Schema(description = "报告编号", example = "2024010100001")
  private Long taskId;

  @Schema(description = "检测类型", example = "1")
  private Integer taskType;

  @Schema(description = "报告状态", example = "3")
  private Integer reportStatus;

  @Schema(description = "创建时间")
  @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
  private LocalDateTime[] createTime;
}
