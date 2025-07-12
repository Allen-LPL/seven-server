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
public enum TaskTypeEnum {

    INNER_QUERY(1, "片内"),


    STRATEGY_QUERY(2, "策略查"),


    FULL_DB_QUERY(3, "全库查");

    private final Integer code;
    private final String desc;

}
