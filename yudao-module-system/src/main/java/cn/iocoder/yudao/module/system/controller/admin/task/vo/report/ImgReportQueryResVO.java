package cn.iocoder.yudao.module.system.controller.admin.task.vo.report;


import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ImgReportQueryResVO {

  private Long id;

  private Long taskId;

  private Integer reportType;

  private String reportName;

  private String reportPath;

  private Integer status;

  private Integer deleted;

  private Long reviewerId;

  private LocalDateTime reviewTime;

}
