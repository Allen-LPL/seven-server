package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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

  @Schema(description = "算法")
  private List<String> modelNameList;

  @Schema(description = "图像类型")
  private List<String> imageTypeList;

  @Schema(description = "特征点数量")
  private Integer featurePoints;

  @Schema(description = "相似度")
  private Double similarScoreThreshold;

}
