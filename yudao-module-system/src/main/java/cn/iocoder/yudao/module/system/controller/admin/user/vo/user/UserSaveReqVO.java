package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.validation.Mobile;
import cn.iocoder.yudao.module.system.framework.operatelog.core.DeptParseFunction;
import cn.iocoder.yudao.module.system.framework.operatelog.core.PostParseFunction;
import cn.iocoder.yudao.module.system.framework.operatelog.core.SexParseFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.util.List;
import java.util.Set;

@Schema(description = "管理后台 - 用户创建/修改 Request VO")
@Data
public class UserSaveReqVO {

    @Schema(description = "用户编号", example = "1024")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "yudao")
    @NotBlank(message = "用户账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "用户账号由 数字、字母 组成")
    @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
    @DiffLogField(name = "用户账号")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    @DiffLogField(name = "用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "部门编号", example = "我是一个用户")
    @DiffLogField(name = "部门", function = DeptParseFunction.NAME)
    private Long deptId;

    @Schema(description = "岗位编号数组", example = "1")
    @DiffLogField(name = "岗位", function = PostParseFunction.NAME)
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "yudao@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    @DiffLogField(name = "用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @Mobile
    @DiffLogField(name = "手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @DiffLogField(name = "用户性别", function = SexParseFunction.NAME)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    @DiffLogField(name = "用户头像")
    private String avatar;

    @Schema(description = "专业领域", example = "[\"计算机科学与技术\", \"软件工程\"]")
    @DiffLogField(name = "专业领域")
    private List<String> major;

    @Schema(description = "擅长领域", example = "[\"人工智能\", \"机器学习\", \"数据挖掘\"]")
    @DiffLogField(name = "擅长领域")
    private List<String> expertise;

    @Schema(description = "技术职称", example = "senior")
    @DiffLogField(name = "技术职称")
    private String techTitle;

    @Schema(description = "邮箱是否验证", example = "true")
    @DiffLogField(name = "邮箱验证状态")
    private Boolean emailVerified;

    @Schema(description = "用户类型", example = "researcher")
    @Pattern(regexp = "^(admin|normal|researcher|student)$", message = "用户类型必须是 admin、normal、researcher、student 之一")
    @DiffLogField(name = "用户类型")
    private String userType;

    // ========== 仅【创建】时，需要传递的字段 ==========

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @AssertTrue(message = "密码不能为空")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null // 修改时，不需要传递
                || (ObjectUtil.isAllNotEmpty(password)); // 新增时，必须都传递 password
    }

}
