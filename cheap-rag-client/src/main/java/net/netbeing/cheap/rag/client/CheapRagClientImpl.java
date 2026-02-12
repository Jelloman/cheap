/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.rag.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.ChannelOption;
import net.netbeing.cheap.rag.client.dto.IndexStatusResponse;
import net.netbeing.cheap.rag.client.dto.QueryRequest;
import net.netbeing.cheap.rag.client.dto.QueryResponse;
import net.netbeing.cheap.rag.client.exception.CheapRagBadRequestException;
import net.netbeing.cheap.rag.client.exception.CheapRagClientException;
import net.netbeing.cheap.rag.client.exception.CheapRagNotFoundException;
import net.netbeing.cheap.rag.client.exception.CheapRagServerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.Map;

/**
 * Implementation of CheapRagClient using Spring WebClient.
 * Thread-safe and reuses the WebClient instance for efficiency.
 */
public class CheapRagClientImpl implements CheapRagClient
{
    private final WebClient webClient;

    /**
     * Creates a client with a simple base URL.
     *
     * @param baseUrl The base URL of the cheap-rag API (e.g., "http://localhost:8000")
     */
    public CheapRagClientImpl(@NotNull String baseUrl)
    {
        this(CheapRagClientConfig.builder().baseUrl(baseUrl).build());
    }

    /**
     * Creates a client with full configuration.
     *
     * @param config The client configuration
     */
    public CheapRagClientImpl(@NotNull CheapRagClientConfig config)
    {
        this.webClient = createWebClient(config);
    }

    /**
     * Creates a client with a custom WebClient (for advanced users).
     *
     * @param webClient Pre-configured WebClient instance
     */
    public CheapRagClientImpl(@NotNull WebClient webClient)
    {
        this.webClient = webClient;
    }

    private WebClient createWebClient(CheapRagClientConfig config)
    {
        // Configure connection pooling
        ConnectionProvider provider = ConnectionProvider.builder("cheap-rag-pool")
                .maxConnections(config.getMaxConnections())
                .maxIdleTime(config.getMaxIdleTime())
                .build();

        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) config.getConnectTimeout().toMillis())
                .responseTimeout(config.getResponseTimeout());

        // Configure ObjectMapper for snake_case JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // Configure exchange strategies with custom ObjectMapper
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                })
                .build();

        return WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    @Override
    @NotNull
    public QueryResponse query(@NotNull String query)
    {
        QueryRequest request = QueryRequest.builder()
                .query(query)
                .build();
        return query(request);
    }

    @Override
    @NotNull
    public QueryResponse query(@NotNull QueryRequest request)
    {
        return queryAsync(request).block();
    }

    @Override
    @NotNull
    public Mono<QueryResponse> queryAsync(@NotNull QueryRequest request)
    {
        return webClient.post()
                .uri("/api/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
                .bodyToMono(QueryResponse.class);
    }

    @Override
    @NotNull
    public IndexStatusResponse getIndexStatus()
    {
        return webClient.get()
                .uri("/api/index/status")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
                .bodyToMono(IndexStatusResponse.class)
                .block();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public Map<String, Object> healthCheck()
    {
        return webClient.get()
                .uri("/health")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerError)
                .bodyToMono(Map.class)
                .block();
    }

    @Override
    @NotNull
    public Map<String, Object> rebuildIndex()
    {
        // Placeholder implementation for Phase 1
        throw new UnsupportedOperationException("Index rebuild not yet implemented in cheap-rag API");
    }

    private Mono<? extends Throwable> handleClientError(ClientResponse response)
    {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    HttpStatus status = (HttpStatus) response.statusCode();
                    return switch (status) {
                        case NOT_FOUND -> Mono.error(new CheapRagNotFoundException(body));
                        case BAD_REQUEST -> Mono.error(new CheapRagBadRequestException(body));
                        default -> Mono.error(new CheapRagClientException(
                                String.format("HTTP %d: %s", status.value(), body)));
                    };
                });
    }

    private Mono<? extends Throwable> handleServerError(ClientResponse response)
    {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    HttpStatus status = (HttpStatus) response.statusCode();
                    return Mono.error(new CheapRagServerException(
                            String.format("HTTP %d: %s", status.value(), body)));
                });
    }
}
