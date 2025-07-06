package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class ImgSimilarityReviewReqVO {

  @Schema(description = "id")
  private Long id;

  @Schema(description = "审核已经")
  private String reviewComment;

  @Schema(description = "是否相似")
  private Boolean isSimilar;

}
