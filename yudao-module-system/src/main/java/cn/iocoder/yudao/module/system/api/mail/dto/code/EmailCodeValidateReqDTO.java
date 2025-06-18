package cn.iocoder.yudao.module.system.api.mail.dto.code;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.enums.mail.EmailSceneEnum;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 邮箱验证码的校验 Request DTO
 *
 * @author 芋道源码
 */
@Data
public class EmailCodeValidateReqDTO {

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @NotEmpty(message = "邮箱不能为空")
    private String email;
    /**
     * 发送场景
     */
    @NotNull(message = "发送场景不能为空")
    @InEnum(EmailSceneEnum.class)
    private Integer scene;
    /**
     * 验证码
     */
    @NotEmpty(message = "验证码不能为空")
    private String code;

} 