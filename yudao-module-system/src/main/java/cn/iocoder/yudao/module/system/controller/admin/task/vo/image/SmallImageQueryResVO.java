package cn.iocoder.yudao.module.system.controller.admin.task.vo.image;


import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class SmallImageQueryResVO {

  @TableId
  private Long id;

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
