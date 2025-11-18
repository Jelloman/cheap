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

package net.netbeing.cheap.rest;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

@Component
public class RequestResponseLoggingFilter implements WebFilter
{
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain)
    {
        var request = exchange.getRequest();
        var method = request.getMethod();
        var path = request.getPath().toString();

        // Log basic request info immediately
        logger.info("Received request: {} {}", method, path);

        // Wrap request to cache body
        var cachedBodyRequest = new CachedBodyServerHttpRequestDecorator(exchange.getRequest());

        // Wrap response to cache body
        var cachedBodyResponse = new CachedBodyServerHttpResponseDecorator(exchange.getResponse());

        // Create new exchange with decorated request and response
        var decoratedExchange = exchange.mutate()
            .request(cachedBodyRequest)
            .response(cachedBodyResponse)
            .build();

        // Log request body when it's been cached
        return cachedBodyRequest.getBodyAsString()
            .doOnNext(body -> {
                if (!body.isEmpty()) {
                    logger.info("Request body: {}", body);
                }
            })
            .then(chain.filter(decoratedExchange))
            .doOnSuccess(_ -> {
                // Log response info
                var status = cachedBodyResponse.getStatusCode();
                logger.info("Request {} {} completed with status {}", method, path, status);

                // Log response body
                var responseBody = cachedBodyResponse.getCachedBody();
                if (!responseBody.isEmpty()) {
                    logger.info("Response body: {}", responseBody);
                }
            });
    }

    /**
     * Decorator that caches the request body for logging while still making it available to downstream handlers
     */
    private static class CachedBodyServerHttpRequestDecorator extends ServerHttpRequestDecorator
    {
        private final Flux<DataBuffer> cachedBody;
        private final Mono<String> bodyString;

        public CachedBodyServerHttpRequestDecorator(ServerHttpRequest delegate) {
            super(delegate);

            var bodyOutputStream = new ByteArrayOutputStream();

            this.cachedBody = super.getBody()
                .doOnNext(dataBuffer -> {
                    try {
                        //noinspection deprecation
                        Channels.newChannel(bodyOutputStream).write(dataBuffer.toByteBuffer().asReadOnlyBuffer());
                    } catch (Exception e) {
                        logger.error("Error caching request body", e);
                    }
                })
                .cache();

            this.bodyString = this.cachedBody
                .then(Mono.fromCallable(() -> bodyOutputStream.toString(StandardCharsets.UTF_8)))
                .defaultIfEmpty("");
        }

        @Override
        @NonNull
        public Flux<DataBuffer> getBody() {
            return cachedBody;
        }

        public Mono<String> getBodyAsString() {
            return bodyString;
        }
    }

    /**
     * Decorator that caches the response body for logging while still writing it to the client
     */
    private static class CachedBodyServerHttpResponseDecorator extends ServerHttpResponseDecorator
    {
        private final ByteArrayOutputStream cachedBody = new ByteArrayOutputStream();

        public CachedBodyServerHttpResponseDecorator(ServerHttpResponse delegate) {
            super(delegate);
        }

        @Override
        @NonNull
        @SuppressWarnings("deprecation")
        public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
            var flux = Flux.from(body)
                .doOnNext(dataBuffer -> {
                    try {
                        Channels.newChannel(cachedBody).write(dataBuffer.toByteBuffer().asReadOnlyBuffer());
                    } catch (Exception e) {
                        logger.error("Error caching response body", e);
                    }
                });

            return super.writeWith(flux);
        }

        public String getCachedBody() {
            return cachedBody.toString(StandardCharsets.UTF_8);
        }
    }
}
