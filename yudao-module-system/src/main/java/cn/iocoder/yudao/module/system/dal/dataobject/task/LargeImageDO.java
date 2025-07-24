package cn.iocoder.yudao.module.system.dal.dataobject.task;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@TableName(value = "iisd_large_image", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LargeImageDO extends BaseDO {

  @TableId
  private Long id;

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
  private Integer status = 1;

  /**
   *  是否底库(1是,0否)
   */
  private Integer isSource = 1;

  private String imageType;


}
