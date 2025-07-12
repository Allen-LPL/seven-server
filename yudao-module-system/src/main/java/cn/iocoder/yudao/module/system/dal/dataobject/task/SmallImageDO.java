package cn.iocoder.yudao.module.system.dal.dataobject.task;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@TableName(value = "iisd_small_image", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmallImageDO extends BaseDO {

  @TableId
  private Long id;

  private Long imageId;

  private Long largeImageId;

  private Long articleId;

  private String imageName;

  private String imageType;

  private String imageFormat;

  private Long imageSize;

  private String vectorPath;

  private String imagePath;

  private Integer status;

  /**
   *  是否底库(1是,0否)
   */
  private Integer isSource = 1;
}
