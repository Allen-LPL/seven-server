package cn.iocoder.yudao.module.system.dal.redis.oauth2;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;

import static cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants.OAUTH2_ONE_CLICK_TICKET;

/**
 * 一键登录票据的 RedisDAO
 */
@Repository
public class OneClickLoginTicketRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String ticket, TicketValue value, Duration ttl) {
        String redisKey = formatKey(ticket);
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(value), ttl);
    }

    public TicketValue get(String ticket) {
        String redisKey = formatKey(ticket);
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        return JsonUtils.parseObject(json, TicketValue.class);
    }

    public void delete(String ticket) {
        stringRedisTemplate.delete(formatKey(ticket));
    }

    private static String formatKey(String ticket) {
        return String.format(OAUTH2_ONE_CLICK_TICKET, ticket);
    }

    @Data
    public static class TicketValue {
        private Long userId;
        private Integer userType;
        private String clientId;
        private String redirectUri;
        private String scope; // space 分隔或自定义
        private String state;
    }
}


