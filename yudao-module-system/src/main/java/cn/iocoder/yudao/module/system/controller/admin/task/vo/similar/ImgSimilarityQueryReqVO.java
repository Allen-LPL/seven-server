package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

  @Schema(description = "特征点数量（支持多个）")
  private List<Integer> featurePoints;

  @Schema(description = "相似度")
  private Double similarScoreThreshold;

  @Schema(description = "是否相似")
  private Boolean isSimilar;

  private Integer featurePointStart;

  private Integer featurePointEnd;

  /**
   * 根据传入的多个特征点值，智能计算起止区间。
   * 规则：
   * - 2  -> [1, 6]
   * - 6  -> [6, 26]
   * - 100 -> [26, null]
   * 多个选择时，合并各自区间的最小 start 和最大 end（end 为 null 表示无上限）。
   */
  public void computeFeaturePointRange() {
    if (featurePoints == null || featurePoints.isEmpty()) {
      this.featurePointStart = null;
      this.featurePointEnd = null;
      return;
    }

    Integer start = null;
    Integer end = null;

    for (Integer fp : featurePoints) {
      if (Objects.equals(fp, 2)) {
        start = start == null ? 1 : Math.min(start, 1);
        end = mergeEnd(end, 6);
      } else if (Objects.equals(fp, 6)) {
        start = start == null ? 6 : Math.min(start, 6);
        end = mergeEnd(end, 26);
      } else if (Objects.equals(fp, 100)) {
        start = start == null ? 26 : Math.min(start, 26);
        end = null; // 无上限
      }
    }

    this.featurePointStart = start;
    this.featurePointEnd = end;
  }

  private Integer mergeEnd(Integer currentEnd, Integer newEnd) {
    if (currentEnd == null) {
      return newEnd;
    }
    if (newEnd == null) {
      return currentEnd;
    }
    return Math.max(currentEnd, newEnd);
  }
}
