package cn.iocoder.yudao.module.system.controller.admin.task.vo.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 报告分页查询 Response VO")
@Data
public class ReportPageRespVO {

    @Schema(description = "报告ID", required = true, example = "1024")
    private Long id;

    @Schema(description = "任务ID", required = true, example = "2048")
    private Long taskId;

    @Schema(description = "报告名称", required = true, example = "2023年度报告")
    private String reportName;

    @Schema(description = "报告路径", example = "/report/2023/annual.pdf")
    private String reportPath;

    @Schema(description = "报告状态", example = "3")
    private Integer status;

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    @Schema(description = "检测策略", example = "1")
    private Integer taskType;

    @Schema(description = "审核结果", example = "2")
    private Integer reviewResult;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "用户姓名", example = "张三")
    private String userName;

    @Schema(description = "用户单位", example = "某某大学")
    private String userUnit;

    @Schema(description = "审核专家姓名", example = "李四")
    private String reviewUserName;

    @Schema(description = "审核专家单位", example = "某某研究所")
    private String reviewUserUnit;

    @Schema(description = "上传文件题目")
    private Map<Long, String> articleTitleMap;

    @Schema(description = "杂志名")
    private Map<Long, String> articleJournalMap;

    @Schema(description = "作者")
    private Map<Long, List<String>> authorNameMap;

    @Schema(description = "相似图片数量", example = "5")
    private Integer similarImages;

    @Schema(description = "首张图片路径列表")
    private List<String> firstImage;

    @Schema(description = "图片总数", example = "10")
    private Integer totalImages;
}
