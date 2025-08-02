package cn.iocoder.yudao.module.system.dal.dataobject.task;


import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TableName(value = "iisd_img_task", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageTaskDO extends BaseDO {

  private Long id;

  private Long taskId;

  /**
   * 任务类型(1篇内自检,2策略检测,3全库检测)
   */
  private Integer taskType;

  private Long creatorId;

  private Long adminId;

  private Long reviewerId;

  /**
   * 策略配置JSON
   */
  private String strategyConfig;

  /**
   * 任务状态(1上传成功,2算法检测中,3审核中,4审核完成) 默认1
   */
  private Integer taskStatus;

  /**
   * 审核结果(1检测中,2检测无异常,3检测有异常) 默认1
   */
  private Integer reviewResult;

  /**
   * 开始时间 算法
   */
  private LocalDateTime startTime;

  /**
   * 算法结束时间
   */
  private LocalDateTime endTime;

  /**
   * 分配时间
   */
  private LocalDateTime adminTime;

  /**
   * 专家审核完成时间
   */
  private LocalDateTime reviewTime;

  /**
   * 总图片数量
   */
  private Integer totalImages;

  /**
   * 已处理图片数量
   */
  private Integer processedImages;

  /**
   * 相似图片数量
   */
  private Integer similarImages;

  private String fileType;

  private String taskNo;

  private String modelList;

  private String imageTypeList;

  private Integer featurePoints = 5;

  private Double similarThreshold;

}
