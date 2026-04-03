package com.rallymate.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SmsRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String SMS_CODE_PREFIX = "auth:sms:";

    @Value("${sms.expiration}")
    private long expiration;

    public void save(String phoneNumber, String verifyCode) {
        redisTemplate.opsForValue().set(
                SMS_CODE_PREFIX + phoneNumber,
                verifyCode,
                Duration.ofSeconds(expiration)
        );
    }

    public Optional<String> find(String phoneNumber) {
        Object value = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phoneNumber);
        return Optional.ofNullable(value).map(Object::toString);
    }

    public void delete(String phoneNumber) {
        redisTemplate.delete(SMS_CODE_PREFIX + phoneNumber);
    }
}
