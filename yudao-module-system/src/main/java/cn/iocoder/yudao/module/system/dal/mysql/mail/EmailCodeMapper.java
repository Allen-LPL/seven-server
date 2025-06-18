package cn.iocoder.yudao.module.system.dal.mysql.mail;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.mail.EmailCodeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱验证码 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EmailCodeMapper extends BaseMapperX<EmailCodeDO> {

    default EmailCodeDO selectLastByEmail(String email, String code, Integer scene) {
        LambdaQueryWrapper<EmailCodeDO> wrapper = new LambdaQueryWrapper<EmailCodeDO>()
                .eq(EmailCodeDO::getEmail, email)
                .orderByDesc(EmailCodeDO::getId);
        if (code != null) {
            wrapper.eq(EmailCodeDO::getCode, code);
        }
        if (scene != null) {
            wrapper.eq(EmailCodeDO::getScene, scene);
        }
        return selectOne(wrapper.last("LIMIT 1"));
    }

} 