package com.example.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan({"com.example.demo.redis"})
public class JedisConfiguration {

    @Autowired
    private RedisConfig redisConfig;

    @Autowired
    private ShardInfoConfig shardInfoConfig;

    @Bean
    public ShardedJedisPool convertJedisPool() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(redisConfig.getMaxTotal());
        jedisPoolConfig.setMaxIdle(redisConfig.getMaxIdle());
        jedisPoolConfig.setMaxWaitMillis(redisConfig.getMaxWaitMillis());
        jedisPoolConfig.setTestOnBorrow(redisConfig.getTestOnBorrow());
        List<JedisShardInfo> jedisShardInfoList = new ArrayList<>();
        jedisShardInfoList.add(new JedisShardInfo(shardInfoConfig.getHost(), shardInfoConfig.getPort(), shardInfoConfig.getName() ));
        return new ShardedJedisPool(jedisPoolConfig, jedisShardInfoList);
    }

    @Bean
    public JedisConnectionFactory getJedisConnectionFactory(){
//        String[] strServer = redisArguments.getServerName().split(":");
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(shardInfoConfig.getHost());
        jedisConnectionFactory.setPort(shardInfoConfig.getPort());
        jedisConnectionFactory.setTimeout(shardInfoConfig.getTimeout());
        return jedisConnectionFactory;
    }
}