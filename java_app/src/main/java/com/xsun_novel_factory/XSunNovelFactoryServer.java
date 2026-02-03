package com.xsun_novel_factory;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@MapperScan(basePackages = {"com.xsun_novel_factory.model.mapper"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.xsun_novel_factory.feign"})
@SpringBootApplication(scanBasePackages = {"com.xsun_novel_factory"})
@EnableAsync
@EnableConfigurationProperties
public class XSunNovelFactoryServer {
    public static void main(String[] args) {
        SpringApplication.run(XSunNovelFactoryServer.class, args);
    }

}
