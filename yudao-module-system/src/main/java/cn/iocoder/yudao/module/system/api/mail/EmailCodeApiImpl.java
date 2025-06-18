package cn.iocoder.yudao.module.system.api.mail;

import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeUseReqDTO;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeValidateReqDTO;
import cn.iocoder.yudao.module.system.service.mail.EmailCodeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 邮箱验证码 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class EmailCodeApiImpl implements EmailCodeApi {

    @Resource
    private EmailCodeService emailCodeService;

    @Override
    public void sendEmailCode(EmailCodeSendReqDTO reqDTO) {
        emailCodeService.sendEmailCode(reqDTO);
    }

    @Override
    public void useEmailCode(EmailCodeUseReqDTO reqDTO) {
        emailCodeService.useEmailCode(reqDTO);
    }

    @Override
    public void validateEmailCode(EmailCodeValidateReqDTO reqDTO) {
        emailCodeService.validateEmailCode(reqDTO);
    }

} 