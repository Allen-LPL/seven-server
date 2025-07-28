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

        //Resnet50
    //ResNet50("ResNet50", 2048, 0.99993),

    DINOv2("dinov2","DINOv2_original","DINOv2_L2", "dinov2_vectors",384, 0.965),

    ResNet50("resnet50","ResNet50_original","ResNet50_L2","resnet50_vectors", 2048, 0.99993),

    CLIP("clip","CLIP_original","CLIP_L2","clip_vectors", 768, 0.965),

    DenseNet121("densenet121","DenseNet121_original", "DenseNet121_L2", "densenet121_vectors",1024, 0.88),

    SwinTransformer("swin_transformer","SwinTransformer_original","SwinTransformer_L2", "swintransformer_vectors",768, 0.92),

    ;

    private final String code;
    private final String origVectorName;
    private final String l2VectorName;
    private final String collectionName;
    private final Integer dim;
    private final Double score;

}
