package com.asong.songbi.manager;

import com.asong.songbi.common.ErrorCode;
import com.asong.songbi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author xys
 */
@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;
    
    @Test
    void doRateLimit() throws InterruptedException {
        String userId = "1";
        for (int i = 0; i < 5; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功"+i);
        }
        Thread.sleep(1100);
        for (int i = 5; i < 10; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功"+i);
        }
    }
    
}