package com.nuonuo.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 小糯智肤后端应用启动入口。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@EnableAsync
@MapperScan("com.nuonuo.**.mapper")
@SpringBootApplication(scanBasePackages = "com.nuonuo")
public class NuonuoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NuonuoApplication.class, args);
    }
}
