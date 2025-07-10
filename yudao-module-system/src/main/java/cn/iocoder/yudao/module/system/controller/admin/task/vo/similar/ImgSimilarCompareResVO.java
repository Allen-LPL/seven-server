package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;


@Data
public class ImgSimilarCompareResVO {

  @Schema(description = "id")
  private Long id;

  @Schema(description = "线图")
  private String dotImage;

  @Schema(description = "块图")
  private String blockImage;
}
