package cn.iocoder.yudao.module.system.service.mail;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeUseReqDTO;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeValidateReqDTO;

import javax.validation.Valid;

/**
 * 邮箱验证码 Service 接口
 *
 * @author 芋道源码
 */
public interface EmailCodeService {

    /**
     * 创建邮箱验证码，并进行发送
     *
     * @param reqDTO 发送请求
     */
    void sendEmailCode(@Valid EmailCodeSendReqDTO reqDTO);

    /**
     * 验证邮箱验证码，并进行使用
     * 如果正确，则将验证码标记成已使用
     * 如果错误，则抛出 {@link ServiceException} 异常
     *
     * @param reqDTO 使用请求
     */
    void useEmailCode(@Valid EmailCodeUseReqDTO reqDTO);

    /**
     * 检查验证码是否有效
     *
     * @param reqDTO 校验请求
     */
    void validateEmailCode(@Valid EmailCodeValidateReqDTO reqDTO);

} 