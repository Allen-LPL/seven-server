package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import java.util.List;
import lombok.Data;


@Data
public class ImageTaskCreateResVO {

  private List<String> failedFile;

  private String failedMsg;

}
