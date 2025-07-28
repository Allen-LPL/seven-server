package cn.iocoder.yudao.module.system.controller.admin.task.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 批量更新 iisd_article 表信息请求 VO")
@Data
public class FileUpdateReqVO {

    @Schema(description = "文件更新列表", required = true)
    @NotEmpty(message = "更新列表不能为空")
    private List<FileUpdateItem> files;

    @Schema(description = "待更新的 iisd_article 表记录项")
    @Data
    public static class FileUpdateItem {
        @Schema(description = "iisd_article 表中的编号", required = true, example = "1024")
        @NotNull(message = "记录编号不能为空")
        private Long id;

        @Schema(description = "文章标题", example = "探讨人工智能在医学影像中的应用")
        private String articleTitle;

        @Schema(description = "杂志名称", example = "中华医学杂志")
        private String articleJournal;

        @Schema(description = "作者列表", example = "[\"张三\", \"李四\"]")
        private List<String> authorName;
    }
} 
