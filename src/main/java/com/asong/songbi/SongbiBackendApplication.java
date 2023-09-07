package com.asong.songbi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

/**
 * @author xys
 */
@MapperScan("com.asong.songbi.mapper")
@SpringBootApplication
public class SongbiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SongbiBackendApplication.class, args);
    }

}
