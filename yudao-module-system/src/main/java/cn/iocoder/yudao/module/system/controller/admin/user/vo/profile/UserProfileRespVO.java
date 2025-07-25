package cn.iocoder.yudao.module.system.controller.admin.user.vo.profile;

import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSimpleRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "管理后台 - 用户个人中心信息 Response VO")
public class UserProfileRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "yudao")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    private String nickname;

    @Schema(description = "用户邮箱", example = "yudao@iocoder.cn")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
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

    @Schema(description = "最后登录 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "192.168.1.1")
    private String loginIp;

    @Schema(description = "最后登录时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    /**
     * 所属角色
     */
    private List<RoleSimpleRespVO> roles;
    /**
     * 所在部门
     */
    private DeptSimpleRespVO dept;
    /**
     * 所属岗位数组
     */
    private List<PostSimpleRespVO> posts;

}
