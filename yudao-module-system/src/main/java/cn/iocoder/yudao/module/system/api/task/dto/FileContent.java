package cn.iocoder.yudao.module.system.api.task.dto;

import lombok.Data;

@Data
public class FileContent {

  private String fileName;
  private String filePath;
  private Long fileSize;

}
