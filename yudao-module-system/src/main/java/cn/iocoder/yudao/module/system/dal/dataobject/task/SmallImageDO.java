package cn.iocoder.yudao.module.system.dal.dataobject.task;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

  private Long imageId;

  private Long largeImageId;

  private Long articleId;

  private String imageName;

  private String imageType;

  private String imageFormat;

  private String imageSize;

  private String vectorId;

  private Integer status;
}
