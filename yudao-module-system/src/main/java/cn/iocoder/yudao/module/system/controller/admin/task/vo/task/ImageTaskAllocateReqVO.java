package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class ImageTaskAllocateReqVO {

  @Schema(description = "任务id", example = "10")
  private Long id;


  @Schema(description = "专家用户id", example = "2")
  private Long adminId;

}