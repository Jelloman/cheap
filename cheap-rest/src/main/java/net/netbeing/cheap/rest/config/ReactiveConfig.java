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

package net.netbeing.cheap.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Configuration for reactive execution.
 * Provides a dedicated scheduler for blocking JDBC operations.
 */
@Configuration
public class ReactiveConfig
{
    /**
     * Creates a bounded elastic scheduler for blocking database operations.
     * This scheduler is designed for blocking I/O and won't block the reactive event loop.
     *
     * The bounded elastic scheduler creates threads on demand up to a configured limit,
     * and recycles them after a TTL period. This is ideal for wrapping blocking calls
     * (like JDBC) in a reactive application.
     *
     * @return a Scheduler instance for blocking operations
     */
    @Bean
    public Scheduler jdbcScheduler()
    {
        // Create a bounded elastic scheduler with thread pool for JDBC operations
        // This prevents blocking the reactive event loop
        return Schedulers.boundedElastic();
    }

    @Bean
    public ForwardedHeaderTransformer forwardedHeaderTransformer()
    {
        return new ForwardedHeaderTransformer();
    }
}
