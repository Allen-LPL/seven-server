package cn.iocoder.yudao.module.system.dal.redis.oauth2;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants.OAUTH2_REFRESH_TOKEN;

@Repository
public class OAuth2RefreshTokenRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static OAuth2RefreshTokenRedisDAO get() {
        return SpringUtils.getBean(OAuth2RefreshTokenRedisDAO.class);
    }

    public OAuth2RefreshTokenDO get(String refreshToken) {
        String key = formatKey(refreshToken);
        String json = stringRedisTemplate.opsForValue().get(key);
        return JsonUtils.parseObject(json, OAuth2RefreshTokenDO.class);
    }

    public void set(OAuth2RefreshTokenDO refreshTokenDO) {
        String key = formatKey(refreshTokenDO.getRefreshToken());
        long seconds = LocalDateTimeUtil.between(LocalDateTime.now(), refreshTokenDO.getExpiresTime(), ChronoUnit.SECONDS);
        if (seconds > 0) {
            stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonString(refreshTokenDO), seconds, TimeUnit.SECONDS);
        }
    }

    public void delete(String refreshToken) {
        stringRedisTemplate.delete(formatKey(refreshToken));
    }

    private static String formatKey(String refreshToken) {
        return String.format(OAUTH2_REFRESH_TOKEN, refreshToken);
    }
}


