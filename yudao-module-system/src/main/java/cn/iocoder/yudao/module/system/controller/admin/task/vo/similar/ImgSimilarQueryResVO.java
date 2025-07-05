package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;


@Data
public class ImgSimilarQueryResVO {

  @Schema(description = "id")
  private Long id;

  @Schema(description = "任务id")
  private Long taskId;

  @Schema(description = "原小图id")
  private Long sourceSmallImageId;

  @Schema(description = "原小图url")
  private String sourceSmallImagePath;

  @Schema(description = "原大图id")
  private Long sourceLargeImageId;

  @Schema(description = "原文章id")
  private Long sourceArticleId;

  @Schema(description = "目标大图id")
  private Long targetLargeImageId;

  @Schema(description = "目标文章id")
  private Long targetArticleId;

  @Schema(description = "目标小图id")
  private Long targetSmallImageId;

  @Schema(description = "目标小图url")
  private String targetSmallImagePath;

  @Schema(description = "相似分")
  private Double similarityScore;

  @Schema(description = "模型名称")
  private String algorithmName;

  @Schema(description = "比对细节")
  private String comparisonDetail;

  @Schema(description = "审核用id")
  private Long reviewerId;

  @Schema(description = "审核用户名")
  private String reviewerUserName;

  @Schema(description = "审核状态(1. 正常 2.复用 3.裁切 4.旋转)")
  private Integer reviewStatus;

  @Schema(description = "审核意见")
  private String reviewComment;

  @Schema(description = "审核时间")
  private LocalDateTime reviewTime;

  @Schema(description = "创建用户Id")
  private String creator;

  @Schema(description = "创建用户名")
  private String creatorUserName;
}
