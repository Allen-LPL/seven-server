package cn.iocoder.yudao.module.system.controller.admin.task.vo.image;


import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class LargeImageQueryResVO {

  @TableId
  private Long id;

  /**
   * 大图ID
   */
  private Long largeImageId;

  /**
   * 所属文件ID
   */
  private Long articleId;

  /**
   *图像名称
   */
  private String imageFileName;

  /**
   * 图像存储路径
   */
  private String imagePath;

  /**
   * 图像格式(JPEG/PNG/BMP等)
   */
  private String imageFormat;

  /**
   *  图像大小(KB)
   */
  private Long imageSize;

  /**
   * 图像宽度高度
   */
  private String widthHeight;

  /**
   *  图像在文件中的页码/位置
   */
  private String pageNumber;

  /**
   *  提取方法
   */
  private String extractionMethod;

  /**
   *  是否已处理切分(0未处理,1已处理)
   */
  private Integer isProcessed;

  /**
   *  图像说明文字
   */
  private String caption;

  /**
   *  状态(1正常,0禁用)
   */
  private Integer status;

  private Long smallImageSum;

}
