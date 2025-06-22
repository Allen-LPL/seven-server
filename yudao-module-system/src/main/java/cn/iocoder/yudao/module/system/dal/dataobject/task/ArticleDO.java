package cn.iocoder.yudao.module.system.dal.dataobject.task;


import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TableName(value = "iisd_article", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDO extends BaseDO {

  /**
   * article_id
   */
  @TableId
  private Long articleId;

  /**
   * 任务id
   */
  private Long taskId;

  /**
   * 文件名称
   */
  private String fileName;

  /**
   * 文件存储路径
   */
  private String filePath;

  /**
   * 文件类型(PDF/WORD/WPS/JPG/PNG等)
   */
  private String fileType;

  /**
   * 文件大小(KB)
   */
  private Long fileSize;

  /**
   * 上传用户ID
   */
  private Long uploadUserId;

  /**
   * 是否为图片文件(0否,1是)
   */
  private Integer isImage;

  private String pmid;

  /**
   * 文章名称
   */
  private String articleTitle;

  /**
   * 文章关键词
   */
  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<String> articleKeywords;

  /**
   * 杂志名称
   */
  private String articleJournal;

  /**
   * 作者姓名
   */
  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<String> authorName;

  /**
   * 作者单位
   */
  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<String> authorInstitution;

  /**
   * 发表时间
   */
  private Long articleDate;

  /**
   * 临床学科（改成枚举类型）（增加交叉学科和未知）改成下拉逻辑
   */
  private String medicalSpecialty;

  /**
   * 是否为底库文献(1是,0否)
   */
  private Integer isSource;

  /**
   * 上传时间
   */
  private LocalDateTime uploadTime;

  /**
   * 状态(1正常,0禁用)
   */
  private Integer status;

}
