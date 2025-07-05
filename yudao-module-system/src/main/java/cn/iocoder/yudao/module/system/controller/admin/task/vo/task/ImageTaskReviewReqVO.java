package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class ImageTaskReviewReqVO{

  @Schema(description = "任务id", example = "10")
  private Long id;


  @Schema(description = "审核结果 1.待审核，2审核通过，3审核不通过", example = "2")
  private Integer reviewResult;

}