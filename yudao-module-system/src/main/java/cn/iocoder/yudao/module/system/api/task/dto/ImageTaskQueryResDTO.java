package cn.iocoder.yudao.module.system.api.task.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ImageTaskQueryResDTO {

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

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 修改时间
   */
  private LocalDateTime updateTime;

  /**
   *  是否删除
   */
  private Integer deleted;

  /**
   * 上传文件或图片列表
   */
  private List<String> firstImage;

  /**
   * 用户姓名
   */
  private String userName;

  /**
   * 用户单位
   */
  private String userUnit;

  /**
   * 审核专家
   */
  private String reviewUserName;
  /**
   * 审核专家单位
   */
  private String reviewUserUnit;
  /**
   *  上传文件题目
   */
  private Map<Long, String> articleTitleMap;

  /**
   *  杂志名
   */
  private Map<Long, String> articleJournalMap;

  /**
   * 作者姓名
   */
  private Map<Long, List<String>> authorNameMap;

  /**
   * 作者单位
   */
  private Map<Long, List<String>> authorInstitutionMap;

  private String fileType;

  private String role;

  private List<String> fileUrlList;

  private String taskNo;

  /**
   * 是否为学术不端案例展示（0 否，1 是）
   */
  private Integer isCase;

  /**
   * 报告路径
   */
  private String reportPath;

}
