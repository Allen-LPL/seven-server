package cn.iocoder.yudao.module.system.enums.mail;

import cn.hutool.core.util.ArrayUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 邮箱验证码发送场景的枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum EmailSceneEnum implements ArrayValuable<Integer> {

    REGISTER(1, "email-register", "邮箱注册"),
    LOGIN(2, "email-login", "邮箱登录"),
    RESET_PASSWORD(3, "email-reset-password", "邮箱重置密码");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EmailSceneEnum::getScene).toArray(Integer[]::new);

    /**
     * 验证场景的编号
     */
    private final Integer scene;
    /**
     * 模板编码
     */
    private final String templateCode;
    /**
     * 描述
     */
    private final String description;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static EmailSceneEnum getCodeByScene(Integer scene) {
        return ArrayUtil.firstMatch(o -> o.getScene().equals(scene), values());
    }

} 