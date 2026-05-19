package com.rallymate.chat.redis;

import com.rallymate.matching.constant.MatchingRedisChannels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ChatRedisConfig {

    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatMatchingRedisSubscriber chatMatchingRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatMatchingRedisSubscriber, new ChannelTopic(MatchingRedisChannels.MATCHED));
        container.addMessageListener(chatMatchingRedisSubscriber, new ChannelTopic(MatchingRedisChannels.FINISHED));
        return container;
    }
}
