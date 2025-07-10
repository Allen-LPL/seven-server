package cn.iocoder.yudao.module.system.controller.admin.task.vo.similar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 相似图片审核意见请求 VO")
@Data
public class ImgSimilarCommentReqVO {

    @Schema(description = "相似图片对ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "是否相似", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isSimilar;

    @Schema(description = "审核意见")
    private String reviewComment;
} 