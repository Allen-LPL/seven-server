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

@TableName(value = "iisd_img_report", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImgReportDO extends BaseDO {

  @TableId
  private Long id;

  private Long taskId;

  // 报告类型(1查询报告,2任务报告,3统计报告)
  private Integer reportType;

  private String reportName;

  private String reportPath;

  private Integer status;

  private Long reviewerId;

  private Long creatorId;

  private LocalDateTime reviewTime;

  private String modelList;

  private String imageTypeList;

  private Integer featurePoints = 5;

  private Double similarThreshold;

}
