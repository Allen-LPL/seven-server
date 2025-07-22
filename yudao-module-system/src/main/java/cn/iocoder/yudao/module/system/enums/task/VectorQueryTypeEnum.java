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
public enum VectorQueryTypeEnum {


    IP("ip", "ip"),

    L2("l2", "l2"),

    COSINE("cosine", "cosine"),

    ;

    private final String code;
    private final String desc;

}
