package com.rallymate.matching.redis;

import com.rallymate.matching.constant.MatchingRedisChannels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 매칭 Redis Pub/Sub 리스너 컨테이너 설정입니다.
 */
@Configuration
public class MatchingRedisConfig {

    @Bean
    public RedisMessageListenerContainer matchingRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MatchingRedisSubscriber matchingRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(matchingRedisSubscriber, new ChannelTopic(MatchingRedisChannels.EVENTS));
        container.addMessageListener(matchingRedisSubscriber, new ChannelTopic(MatchingRedisChannels.MATCHED));
        return container;
    }
}
