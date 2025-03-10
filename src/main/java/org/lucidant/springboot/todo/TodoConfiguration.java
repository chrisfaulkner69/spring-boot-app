package org.lucidant.springboot.todo;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.HttpStatusCode;

@Configuration
public class TodoConfiguration {

    @Bean
    public WebClient webClient(@Value("${base.url:defaultUrl}") String baseUrl) {
        final var httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
            .defaultHeaders(header -> header.setContentType(MediaType.APPLICATION_JSON))
            .defaultStatusHandler(HttpStatusCode::isError, resp ->
                Mono.just(new MyServiceException("Custom exception")))
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    public TodoClient todoClient(WebClient webClient) {

        HttpExchangeAdapter exchangeAdapter =  WebClientAdapter.create(webClient);

        var factory = HttpServiceProxyFactory
                .builder()
                .exchangeAdapter(exchangeAdapter)
                .build();

        return factory.createClient(TodoClient.class);
    }
}
