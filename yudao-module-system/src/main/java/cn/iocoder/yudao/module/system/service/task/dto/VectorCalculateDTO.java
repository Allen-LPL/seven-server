package cn.iocoder.yudao.module.system.service.task.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ResNet50 2048 维
 * DINOv2 384 维
 * CLIP ViT-L/14 768 维
 * DenseNet121 1024 维
 * Swin Transformer 768 维
 *
 *
 * ResNet50 0.99993
 * CLIP  0.965
 * DINOv2 0.965
 * DenseNet121 0.88
 * SwinTransformer 0.92
 *
 */
@Data
public class VectorCalculateDTO {

  private Long smallImageId;

  private Long largeImageId;

  private Long articleId;

  private List<Double> resnet50;

  private List<Double> dinoV2;

  private List<Double> clipVit;

  private List<Double> denseNet121;

  private List<Double> swinTransformer;

  private List<ScoreData> similarList;


  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  public static class ScoreData{
    private Long smallImageId;
    private Double score;
  }

}
