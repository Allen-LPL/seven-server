package cn.iocoder.yudao.module.system.controller.admin.task.vo.task;

import cn.iocoder.yudao.module.system.api.task.dto.TaskStrategyConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class ImageTaskCreateReqVO{

  @Schema(description = "文件")
  private MultipartFile[] files;

  @Schema(description = "检测策略", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
  private Integer taskType;

  @Schema(description = "文件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "image/pdf")
  private String fileType;

  @Schema(description = "策略查-任务检测", example = "{}")
  private TaskStrategyConfig taskStrategyConfig;

}