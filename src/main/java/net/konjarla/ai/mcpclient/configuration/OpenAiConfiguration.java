package net.konjarla.ai.mcpclient.configuration;

import net.konjarla.ai.mcpclient.transport.RestClientInterceptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class OpenAiConfiguration {
    @Value("${spring.ai.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String openAiChatModel;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double openAiChatTemperature;

    @Value("${spring.ai.openai.chat.options.maxCompletionTokens}")
    private Integer maxCompletionTokens;

    @Bean
    public OpenAiApi openAiApi() {

        RestClient.Builder builder = RestClient.builder();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings
                .defaults()
                .withConnectTimeout(Duration.ofSeconds(60))
                .withReadTimeout(Duration.ofSeconds(300));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        builder.requestFactory(requestFactory)
                .requestInterceptor(new RestClientInterceptor());
        WebClient.Builder webClientBuilder = WebClient.builder();

        return OpenAiApi.builder()
                .baseUrl(openAiBaseUrl)
                .apiKey(openAiApiKey)
                .restClientBuilder(builder)
                .webClientBuilder(webClientBuilder)
                .build();
    }

    @Bean
    public ChatModel openAiChatModel() {
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model(openAiChatModel)
                .temperature(openAiChatTemperature)
                .maxCompletionTokens(maxCompletionTokens)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi())
                .defaultOptions(openAiChatOptions)
                .build();
    }

    @Bean
    public ChatClient openAiChatClient() {
        return ChatClient.builder(openAiChatModel())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
