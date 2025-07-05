package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class ImgSimilarityQueryReqVO extends PageParam {

  @Schema(description = "taskId")
  private Long taskId;

  @Schema(description = "id")
  private Long id;

  @Schema(description = "creator")
  private Long creator;

}
