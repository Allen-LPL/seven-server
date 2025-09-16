package cn.iocoder.yudao.module.system.service.mail;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeUseReqDTO;
import cn.iocoder.yudao.module.system.api.mail.dto.code.EmailCodeValidateReqDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.mail.EmailCodeDO;
import cn.iocoder.yudao.module.system.dal.mysql.mail.EmailCodeMapper;
import cn.iocoder.yudao.module.system.enums.mail.EmailSceneEnum;
import cn.iocoder.yudao.module.system.framework.mail.config.EmailCodeProperties;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static cn.hutool.core.util.RandomUtil.randomInt;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.isToday;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 邮箱验证码 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class EmailCodeServiceImpl implements EmailCodeService {

    @Resource
    private EmailCodeProperties emailCodeProperties;

    @Resource
    private EmailCodeMapper emailCodeMapper;

    @Resource
    private MailSendService mailSendService;

    @Override
    public void sendEmailCode(EmailCodeSendReqDTO reqDTO) {
        EmailSceneEnum sceneEnum = EmailSceneEnum.getCodeByScene(reqDTO.getScene());
        Assert.notNull(sceneEnum, "验证码场景({}) 查找不到配置", reqDTO.getScene());
        // 创建验证码
        String code = createEmailCode(reqDTO.getEmail(), reqDTO.getScene(), reqDTO.getCreateIp());
        // 发送验证码
        mailSendService.sendSingleMail(reqDTO.getEmail(), null, UserTypeEnum.ADMIN.getValue(),
                sceneEnum.getTemplateCode(), MapUtil.of("code", code));
    }

    private String createEmailCode(String email, Integer scene, String ip) {
        // 校验是否可以发送验证码，不用筛选场景
        EmailCodeDO lastEmailCode = emailCodeMapper.selectLastByEmail(email, null, null);
//        if (lastEmailCode != null) {
//            if (LocalDateTimeUtil.between(lastEmailCode.getCreateTime(), LocalDateTime.now()).toMillis()
//                    < emailCodeProperties.getSendFrequency().toMillis()) { // 发送过于频繁
//                throw exception(EMAIL_CODE_SEND_TOO_FAST);
//            }
//            if (isToday(lastEmailCode.getCreateTime()) && // 必须是今天，才能计算超过当天的上限
//                    lastEmailCode.getTodayIndex() >= emailCodeProperties.getSendMaximumQuantityPerDay()) { // 超过当天发送的上限。
//                throw exception(EMAIL_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
//            }
//        }

        // 创建验证码记录
        String code = String.format("%0" + emailCodeProperties.getEndCode().toString().length() + "d",
                randomInt(emailCodeProperties.getBeginCode(), emailCodeProperties.getEndCode() + 1));
        EmailCodeDO newEmailCode = EmailCodeDO.builder().email(email).code(code).scene(scene)
                .todayIndex(lastEmailCode != null && isToday(lastEmailCode.getCreateTime()) ? lastEmailCode.getTodayIndex() + 1 : 1)
                .createIp(ip).used(false).build();
        emailCodeMapper.insert(newEmailCode);
        return code;
    }

    @Override
    public void useEmailCode(EmailCodeUseReqDTO reqDTO) {
        // 检测验证码是否有效
        EmailCodeDO lastEmailCode = validateEmailCode0(reqDTO.getEmail(), reqDTO.getCode(), reqDTO.getScene());
        // 使用验证码
        emailCodeMapper.updateById(EmailCodeDO.builder().id(lastEmailCode.getId())
                .used(true).usedTime(LocalDateTime.now()).usedIp(reqDTO.getUsedIp()).build());
    }

    @Override
    public void validateEmailCode(EmailCodeValidateReqDTO reqDTO) {
        validateEmailCode0(reqDTO.getEmail(), reqDTO.getCode(), reqDTO.getScene());
    }

    private EmailCodeDO validateEmailCode0(String email, String code, Integer scene) {
        // 校验验证码
        EmailCodeDO lastEmailCode = emailCodeMapper.selectLastByEmail(email, code, scene);
        // 若验证码不存在，抛出异常
        if (lastEmailCode == null) {
            throw exception(EMAIL_CODE_NOT_FOUND);
        }
        // 超过时间
        if (LocalDateTimeUtil.between(lastEmailCode.getCreateTime(), LocalDateTime.now()).toMillis()
                >= emailCodeProperties.getExpireTimes().toMillis()) { // 验证码已过期
            throw exception(EMAIL_CODE_EXPIRED);
        }
        // 判断验证码是否已被使用
        if (Boolean.TRUE.equals(lastEmailCode.getUsed())) {
            throw exception(EMAIL_CODE_USED);
        }
        return lastEmailCode;
    }

} 
