package cn.iocoder.yudao.module.system.controller.admin.task.vo;

import java.util.List;
import lombok.Data;

@Data
public class ManualProcessFileVO {

  private List<String> fileList;

  private String fileType;

}
