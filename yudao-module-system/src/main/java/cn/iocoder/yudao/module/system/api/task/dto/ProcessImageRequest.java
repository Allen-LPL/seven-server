package cn.iocoder.yudao.module.system.api.task.dto;

import java.util.List;
import lombok.Data;

@Data
public class ProcessImageRequest {

  private Long articleId;

  private String filePath;

  private String largePrefixPath;

  private String smallPrefixPath;

  private String fileType;

  private List<String> keywords;

  private String author;

  private String institution;

  private Long articleDate;

  private String specialty;

}
