package com.xsun_novel_factory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class LangChain4jConfig {

    @Bean
    @ConfigurationProperties(prefix = "deepseek")
    public OpenAiProps openAiProps() {
        return new OpenAiProps();
    }


    @Bean
    public ChatModel chatModel(OpenAiProps props) {
        // OpenAiChatModel 支持 builder 方式配置 modelName / apiKey / temperature / timeout 等参数
        return OpenAiChatModel.builder()
                .apiKey(props.getApikey())
                .baseUrl(props.getBaseUrl())
                .modelName(props.getModel())
                .temperature(props.getTemperature())
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }


    @Getter
    @Setter
    public static class OpenAiProps {
        private String apikey;
        private String baseUrl;
        private String model ;
        private Double temperature ;
        private Long timeoutSeconds ;
    }

}