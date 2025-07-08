package cn.iocoder.yudao.module.system.controller.admin.task.vo.image;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class SmallImageUpdateReqVO{

  @Schema(description = "id")
  private Long id;

  @Schema(description = "大图id")
  private Long largeImageId;

  @Schema(description = "文章id")
  private Long articleId;

  @Schema(description = "图片名称")
  private String imageName;

  @Schema(description = "图片类型")
  private String imageType;

  @Schema(description = "图片大小")
  private Long imageSize;

  @Schema(description = "小图向量地址")
  private String vectorPath;

  @Schema(description = "小图url")
  private String imagePath;

  @Schema(description = "状态")
  private Integer status;

}
