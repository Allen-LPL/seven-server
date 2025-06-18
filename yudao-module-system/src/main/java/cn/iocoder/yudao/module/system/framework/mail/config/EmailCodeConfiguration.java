package cn.iocoder.yudao.module.system.framework.mail.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 邮箱验证码的配置类
 *
 * @author 芋道源码
 */
@Configuration
@EnableConfigurationProperties(EmailCodeProperties.class)
public class EmailCodeConfiguration {

} 