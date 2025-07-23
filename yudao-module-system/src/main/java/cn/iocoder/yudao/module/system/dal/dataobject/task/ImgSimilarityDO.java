package cn.iocoder.yudao.module.system.dal.dataobject.task;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Int;

@TableName(value = "iisd_img_similarity", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImgSimilarityDO extends BaseDO {

  private Long id;

  private Long taskId;

  private Long sourceSmallImageId;

  private Long sourceLargeImageId;

  private Long sourceArticleId;

  private Long targetLargeImageId;

  private Long targetArticleId;

  private Long targetSmallImageId;

  private Double similarityScore;

  private String algorithmName;

  private Float vectorDistance;

  private String comparisonDetail;

  private Long reviewerId;

  private Integer reviewStatus;

  private String reviewComment;

  private LocalDateTime reviewTime;

  private Boolean isSimilar;

  private String dotImage;

  private String blockImage;

  private Integer featurePointCnt;

}
