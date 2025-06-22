package cn.iocoder.yudao.module.system.controller.admin.task.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
public class FileQueryResVO {

  @Schema(description = "id")
  private Long id;

  /**
   * 任务id
   */
  @Schema(description = "任务id")
  private Long taskId;

  /**
   * 文件名称
   */
  @Schema(description = "文件名称")
  private String fileName;

  /**
   * 文件存储路径
   */
  @Schema(description = "文件路径")
  private String filePath;

  /**
   * 文件类型(PDF/WORD/WPS/JPG/PNG等)
   */
  @Schema(description = "文件类型")
  private String fileType;

  /**
   * 文件大小(KB)
   */
  @Schema(description = "文件大小(KB)")
  private Long fileSize;

  /**
   * 上传用户ID
   */
  @Schema(description = "上传用户ID")
  private Long uploadUserId;

  /**
   * 是否为图片文件(0否,1是)
   */
  @Schema(description = "是否为图片文件(0否,1是)")
  private Integer isImage;

  private String pmid;

  /**
   * 文章名称
   */
  @Schema(description = "文章名称")
  private String articleTitle;

  @Schema(description = "关键字")
  private List<String> articleKeywords;

  @Schema(description = "文章杂志")
  private String articleJournal;

  @Schema(description = "文章作者")
  private List<String> authorName;

  @Schema(description = "")
  private List<String> authorInstitution;

  /**
   * 发表时间
   */
  @Schema(description = "发表时间")
  private Long articleDate;

  @Schema(description = "medicalSpecialty")
  private String medicalSpecialty;

  /**
   * 是否为底库文献(1是,0否)
   */
  @Schema(description = "是否为底库文献(1是,0否)")
  private Integer isSource;

  /**
   * 上传时间
   */
  @Schema(description = "上传时间")
  private LocalDateTime uploadTime;

  /**
   * 状态(1正常,0禁用)
   */
  @Schema(description = "状态(1正常,0禁用)")
  private Integer status;

  @Schema(description = "废弃文章ID")
  private Long articleId;

  @Schema(description = "大图数量")
  private Long largeImageSum;

  @Schema(description = "小图数量")
  private Long smallImageSum;

}
