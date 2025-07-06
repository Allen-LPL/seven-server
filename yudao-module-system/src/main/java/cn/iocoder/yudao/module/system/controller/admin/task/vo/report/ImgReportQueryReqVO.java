package cn.iocoder.yudao.module.system.controller.admin.task.vo.report;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class ImgReportQueryReqVO extends PageParam {

  @Schema(description = "上传开始时间")
  private Long startTime;

  @Schema(description = "上传结束时间")
  private Long endTime;

  @Schema(description = "id")
  private Long id;

  @Schema(description = "creator")
  private Long creator;

  @Schema(description = "updater")
  private Long updater;

}
