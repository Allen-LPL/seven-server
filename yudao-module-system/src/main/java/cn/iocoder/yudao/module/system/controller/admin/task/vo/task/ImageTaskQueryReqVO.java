package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ImageTaskQueryReqVO extends PageParam {

  @Schema(description = "关键词", example = "[]")
  private List<String> keyword;

  @Schema(description = "创建人", example = "1")
  private Long creatorId;

  @Schema(description = "审核人", example = "1")
  private Long reviewId;

  @Schema(description = "状态", example = "1")
  private Integer status;

  @Schema(description = "审核结果", example = "1")
  private Integer reviewResult;

  @Schema(description = "上传开始时间")
  private Long startTime;

  @Schema(description = "上传结束时间")
  private Long endTime;

  @Schema(description = "检测策略", example = "1")
  private Integer taskType;

  @Schema(description = "任务id", example = "1")
  private Long taskId;

  @Schema(description = "仅展示案例", example = "true")
  private Boolean caseOnly;

}