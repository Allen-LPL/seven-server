package cn.iocoder.yudao.module.system.api.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TaskStrategyConfig implements Serializable {
  @Schema(description = "策略查-开始时间")
  private LocalDateTime startTime;

  @Schema(description = "策略查-结束时间")
  private LocalDateTime endTime;

  @Schema(description = "策略查-关键词列表", example = "[]")
  private List<String> keywordList;

  @Schema(description = "策略查-学科类别", example = "1")
  private String medicalSpecialty;

}
