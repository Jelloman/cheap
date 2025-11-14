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

package net.netbeing.cheap.rest.controller;

import net.netbeing.cheap.json.dto.AspectQueryRequest;
import net.netbeing.cheap.json.dto.AspectQueryResponse;
import net.netbeing.cheap.json.dto.UpsertAspectsRequest;
import net.netbeing.cheap.json.dto.UpsertAspectsResponse;
import net.netbeing.cheap.json.dto.UpsertAspectsResponse.AspectResult;
import net.netbeing.cheap.rest.service.ReactiveAspectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Aspect operations.
 * Uses reactive types (Mono) to provide non-blocking HTTP handling.
 */
@RestController
@RequestMapping("/api/catalog/{catalogId}/aspects")
public class AspectController
{
    private static final Logger logger = LoggerFactory.getLogger(AspectController.class);

    private final ReactiveAspectService aspectService;
    
    @Value("${cheap.aspect-upsert.max-batch-size:1000}")
    private int maxBatchSize;

    public AspectController(ReactiveAspectService aspectService)
    {
        this.aspectService = aspectService;
    }

    /**
     * Upserts aspects for multiple entities reactively.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the AspectDef name
     * @param request the upsert request
     * @return Mono emitting the upsert results
     */
    @PostMapping("/{aspectDefName}")
    public Mono<ResponseEntity<UpsertAspectsResponse>> upsertAspects(
        @PathVariable UUID catalogId,
        @PathVariable String aspectDefName,
        @RequestBody UpsertAspectsRequest request)
    {
        logger.info("Received request to upsert {} aspects of type {} in catalog {}",
            request.aspects().size(), aspectDefName, catalogId);

        // Validate batch size
        if (request.aspects().size() > maxBatchSize) {
            throw new IllegalArgumentException(
                "Batch size " + request.aspects().size() + " exceeds maximum of " + maxBatchSize
            );
        }

        // Convert request to service format
        Map<UUID, Map<String, Object>> aspectsByEntity = new LinkedHashMap<>();
        for (UpsertAspectsRequest.AspectData aspectData : request.aspects()) {
            aspectsByEntity.put(aspectData.entityId(), aspectData.properties());
        }

        // Call service and convert results
        return aspectService.upsertAspects(catalogId, aspectDefName, aspectsByEntity)
            .map(results -> {
                // Convert results to response format
                List<AspectResult> resultList = new ArrayList<>();
                int successCount = 0;
                int failureCount = 0;

                for (AspectResult result : results.values()) {
                    resultList.add(new AspectResult(
                        result.entityId(),
                        result.success(),
                        result.created(),
                        result.message()
                    ));

                    if (result.success()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }

                UpsertAspectsResponse response = new UpsertAspectsResponse(
                    catalogId,
                    aspectDefName,
                    resultList,
                    successCount,
                    failureCount
                );

                // Return 200 if all succeeded, 207 if partial success
                HttpStatus status = failureCount > 0 ? HttpStatus.MULTI_STATUS : HttpStatus.OK;
                return ResponseEntity.status(status).body(response);
            });
    }

    /**
     * Queries aspects for multiple entities and AspectDefs reactively.
     *
     * @param catalogId the catalog ID
     * @param request the query request containing entity IDs and AspectDef names
     * @return Mono emitting map of entity IDs to maps of AspectDef names to aspects
     */
    @PostMapping("/query")
    public Mono<AspectQueryResponse> queryAspects(
        @PathVariable UUID catalogId,
        @RequestBody AspectQueryRequest request)
    {
        logger.info("Received request to query {} entities for {} AspectDefs in catalog {}",
            request.entityIds().size(), request.aspectDefNames().size(), catalogId);

        return aspectService.queryAspects(
                catalogId,
                request.entityIds(),
                request.aspectDefNames()
            )
            .map(results -> new AspectQueryResponse(catalogId, results));
    }
}
