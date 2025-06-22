package cn.iocoder.yudao.module.system.api.task.dto;

import java.util.List;
import lombok.Data;

@Data
public class ImageTaskCreateResDTO {

  private Boolean success = Boolean.TRUE;

  private List<String> failedFile;

  private List<FileContent> successFile;

  private String failedMsg;

}
