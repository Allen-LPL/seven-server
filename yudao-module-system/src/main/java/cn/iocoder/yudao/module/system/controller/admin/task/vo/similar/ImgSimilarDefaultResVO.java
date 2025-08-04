package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;

import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultFeaturePointsDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultImageTypeDTO;
import cn.iocoder.yudao.module.system.controller.admin.task.dto.DefaultModelDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 相似图片默认值 Response VO")
@Data
public class ImgSimilarDefaultResVO {
  @Schema(description = "默认算法列表")
  private List<DefaultModelDTO> defaultModelList;
  @Schema(description = "默认图片分类列表")
  private List<DefaultImageTypeDTO> defaultImageTypeList;
  @Schema(description = "默认特征点列表")
  private List<DefaultFeaturePointsDTO> defaultFeaturePointsList;
  @Schema(description = "用户角色列表")
  private List<String> roles;
}
