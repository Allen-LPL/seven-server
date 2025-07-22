package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;

import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultFeaturePointsDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultImageTypeDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultModelDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;


@Data
public class ImgSimilarDefaultResVO {

  @Schema(description = "算法列表&及对应的默认相似分")
  private List<DefaultModelDTO> defaultModelList;

  @Schema(description = "图像分类列表")
  private List<DefaultImageTypeDTO> defaultImageTypeList;

  @Schema(description = "特征点数量列表")
  private List<DefaultFeaturePointsDTO> defaultFeaturePointsList;
}
