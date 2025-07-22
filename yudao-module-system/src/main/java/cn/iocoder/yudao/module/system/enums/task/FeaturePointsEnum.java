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
public enum FeaturePointsEnum {

    ZERO(">0个", 0),

    TWO(">=2个", 2),

    FIVE(">=5个", 5),

    HUNDRED(">=100个", 100),;

    private final String code;
    private final Integer threshold;

}
