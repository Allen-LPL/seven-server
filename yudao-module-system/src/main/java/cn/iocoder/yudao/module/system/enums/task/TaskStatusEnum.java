package cn.iocoder.yudao.module.system.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知模板类型枚举
 *
 * @author HUIHUI
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {


    UPLOAD_SUCCESS(1, "上传成功"),

    ALGO_DETECT(2, "算法检测"),

    EXPERT_REVIEW(3, "专家审核"),

    COMPLETE(4, "检测完成")

    ;

    private final Integer code;
    private final String desc;

}
