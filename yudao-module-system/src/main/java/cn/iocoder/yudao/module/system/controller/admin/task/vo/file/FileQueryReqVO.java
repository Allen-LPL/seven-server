package cn.iocoder.yudao.module.system.controller.admin.task.vo.file;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FileQueryReqVO extends PageParam {

  @Schema(description = "创建人", example = "1")
  private Long creatorId;

  @Schema(description = "上传开始时间")
  private Long startTime;

  @Schema(description = "上传结束时间")
  private Long endTime;

  @Schema(description = "杂志社")
  private String articleJournal;

  @Schema(description = "关键字")
  private String articleKeywords;

  @Schema(description = "id")
  private Long id;


}
