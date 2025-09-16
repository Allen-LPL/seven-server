package cn.iocoder.yudao.module.system.controller.admin.oauth2.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class OneClickLoginTicketCreateReqVO {

    @Schema(description = "邮箱", example = "user@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "scope，可空，空则使用客户端默认", example = "user_info")
    private String scope;

    @Schema(description = "state，透传，可空", example = "nav_jump_123")
    private String state;

    @Schema(description = "票据有效期秒数，可空，默认60秒", example = "60")
    private Integer ttlSeconds;
}


