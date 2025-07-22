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
public enum ImageTypeEnum {

    MEDICAL("medical", "医学图像"),


    STATISTICS("statistics", "统计学图像"),


    OTHERS("others", "其他类型图像");

    private final String code;
    private final String desc;

}
