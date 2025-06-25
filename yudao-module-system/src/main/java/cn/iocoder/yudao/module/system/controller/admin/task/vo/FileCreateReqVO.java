package cn.iocoder.yudao.module.system.controller.admin.task.vo;

import cn.iocoder.yudao.module.system.api.task.dto.TaskStrategyConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "管理后台 - 邮箱账号分页 Request VO")
@Data
public class FileCreateReqVO {

  @Schema(description = "文件")
  private MultipartFile[] files;

  @Schema(description = "文件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "image/pdf")
  private String fileType;

}