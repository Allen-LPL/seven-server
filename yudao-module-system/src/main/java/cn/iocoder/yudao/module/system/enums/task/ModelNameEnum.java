package cn.iocoder.yudao.module.system.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;


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
@Getter
@AllArgsConstructor
public enum ModelNameEnum {


    ResNet50("ResNet50", 2048, 0.99993),

    DINOv2("DINOv2", 384, 0.965),

    CLIP("CLIP", 768, 0.965),

    DenseNet121("DenseNet121", 1024, 0.88),

    SwinTransformer("SwinTransformer", 768, 0.92),

    ;

    private final String code;
    private final Integer dim;
    private final Double score;

}
