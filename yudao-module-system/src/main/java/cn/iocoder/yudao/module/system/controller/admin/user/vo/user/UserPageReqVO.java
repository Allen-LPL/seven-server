package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 用户分页 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPageReqVO extends PageParam {

    @Schema(description = "用户账号，模糊匹配", example = "yudao")
    private String username;

    @Schema(description = "手机号码，模糊匹配", example = "yudao")
    private String mobile;

    @Schema(description = "展示状态，参见 CommonStatusEnum 枚举类", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "[2022-07-01 00:00:00, 2022-07-01 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "部门编号，同时筛选子部门", example = "1024")
    private Long deptId;

    @Schema(description = "角色编号", example = "1024")
    private Long roleId;

    @Schema(description = "用户邮箱，模糊匹配", example = "test@example.com")
    private String email;

    @Schema(description = "专业，模糊匹配", example = "计算机科学")
    private String major;

    @Schema(description = "擅长领域，模糊匹配", example = "人工智能")
    private String expertise;

    @Schema(description = "用户类型", example = "researcher")
    private String userType;

    @Schema(description = "用户类型列表", example = "[\"Expert_admin\", \"Research_admin\"]")
    private List<String> userTypeList;

    @Schema(description = "邮箱验证状态", example = "true")
    private Boolean emailVerified;

    @Schema(description = "逻辑删除", example = "1")
    private Integer deleted;
}
