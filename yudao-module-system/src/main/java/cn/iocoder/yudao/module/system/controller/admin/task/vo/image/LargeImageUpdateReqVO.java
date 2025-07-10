package cn.iocoder.yudao.module.system.controller.admin.task.vo.image;


import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class LargeImageUpdateReqVO{

  @Schema(description = "id")
  private Long id;


  /**
   * 所属文件ID
   */
  @Schema(description = "所属文件ID")
  private Long articleId;

  /**
   *图像名称
   */
  @Schema(description = "图像名称")
  private String imageFileName;

  /**
   * 图像存储路径
   */
  @Schema(description = "图像存储路径")
  private String imagePath;

  /**
   * 图像格式(JPEG/PNG/BMP等)
   */
  @Schema(description = "图像格式(JPEG/PNG/BMP等)")
  private String imageFormat;

  /**
   *  图像大小(KB)
   */
  @Schema(description = "图像大小(KB)")
  private Long imageSize;

  /**
   * 图像宽度高度
   */
  @Schema(description = "图像宽度高度")
  private String widthHeight;

  /**
   *  图像在文件中的页码/位置
   */
  @Schema(description = "图像在文件中的页码/位置")
  private String pageNumber;

  /**
   *  提取方法
   */
  @Schema(description = "提取方法")
  private String extractionMethod;

  /**
   *  是否已处理切分(0未处理,1已处理)
   */
  @Schema(description = "是否已处理切分(0未处理,1已处理)")
  private Integer isProcessed;

  /**
   *  图像说明文字
   */
  @Schema(description = "图像说明文字")
  private String caption;

  /**
   *  状态(1正常,0禁用)
   */
  @Schema(description = "状态(1正常,0禁用)")
  private Integer status = 1;

}
