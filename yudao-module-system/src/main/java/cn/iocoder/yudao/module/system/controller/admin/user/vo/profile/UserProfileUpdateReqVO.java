package cn.iocoder.yudao.module.system.controller.admin.user.vo.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.List;


@Schema(description = "管理后台 - 用户个人信息更新 Request VO")
@Data
public class UserProfileUpdateReqVO {

    @Schema(description = "用户昵称", example = "芋艿")
    @Size(max = 30, message = "用户昵称长度不能超过 30 个字符")
    private String nickname;

    @Schema(description = "用户邮箱", example = "yudao@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @Length(min = 11, max = 11, message = "手机号长度必须 11 位")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    private Integer sex;

    @Schema(description = "角色头像", example = "https://www.iocoder.cn/1.png")
    @URL(message = "头像地址格式不正确")
    private String avatar;

    @Schema(description = "专业领域", example = "[\"医学信息学\", \"生物信息学\"]")
    private List<String> major;

    @Schema(description = "擅长领域", example = "[\"医学图像处理\", \"数据挖掘\"]")
    private List<String> expertise;

    @Schema(description = "技术职称", example = "senior")
    private String techTitle;

    @Schema(description = "邮箱是否验证", example = "true")
    private Boolean emailVerified;

    @Schema(description = "用户类型", example = "researcher")
    private String userType;

}
