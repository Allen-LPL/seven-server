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
public enum FileTypeEnum {


    PDF("pdf", "pdf"),

    IMAGE("image", "image")

    ;

    private final String code;
    private final String desc;

}
