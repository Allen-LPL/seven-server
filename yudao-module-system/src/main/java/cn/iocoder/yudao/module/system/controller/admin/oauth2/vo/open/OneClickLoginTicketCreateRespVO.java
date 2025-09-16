package cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OneClickLoginTicketCreateRespVO {
    @Schema(description = "一次性票据")
    private String ticket;

    @Schema(description = "过期秒数")
    private Integer expiresIn;
}


