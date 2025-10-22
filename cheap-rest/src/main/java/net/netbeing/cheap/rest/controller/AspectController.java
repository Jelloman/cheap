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

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.rest.dto.AspectQueryRequest;
import net.netbeing.cheap.rest.dto.AspectQueryResponse;
import net.netbeing.cheap.rest.dto.UpsertAspectsRequest;
import net.netbeing.cheap.rest.dto.UpsertAspectsResponse;
import net.netbeing.cheap.rest.service.AspectService;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Aspect operations.
 */
@RestController
@RequestMapping("/api/catalogs/{catalogId}/aspects")
public class AspectController
{
    private static final Logger logger = LoggerFactory.getLogger(AspectController.class);

    private final AspectService aspectService;
    
    @Value("${cheap.aspect-upsert.max-batch-size:1000}")
    private int maxBatchSize;

    public AspectController(AspectService aspectService)
    {
        this.aspectService = aspectService;
    }

    /**
     * Upserts aspects for multiple entities.
     *
     * @param catalogId the catalog ID
     * @param aspectDefName the AspectDef name
     * @param request the upsert request
     * @return the upsert results
     */
    @PostMapping("/{aspectDefName}")
    public ResponseEntity<UpsertAspectsResponse> upsertAspects(
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

        // Default createEntities to true if not specified
        boolean createEntities = request.createEntities() != null ? request.createEntities() : true;

        // Call service
        Map<UUID, AspectService.UpsertResult> results = aspectService.upsertAspects(
            catalogId,
            aspectDefName,
            aspectsByEntity,
            createEntities
        );

        // Convert results to response format
        List<UpsertAspectsResponse.AspectResult> resultList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (AspectService.UpsertResult result : results.values()) {
            resultList.add(new UpsertAspectsResponse.AspectResult(
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
    }

    /**
     * Queries aspects for multiple entities and AspectDefs.
     *
     * @param catalogId the catalog ID
     * @param request the query request containing entity IDs and AspectDef names
     * @return map of entity IDs to maps of AspectDef names to aspects
     */
    @PostMapping("/query")
    public ResponseEntity<AspectQueryResponse> queryAspects(
        @PathVariable UUID catalogId,
        @RequestBody AspectQueryRequest request)
    {
        logger.info("Received request to query {} entities for {} AspectDefs in catalog {}",
            request.entityIds().size(), request.aspectDefNames().size(), catalogId);

        Map<UUID, Map<String, Aspect>> results = aspectService.queryAspects(
            catalogId,
            request.entityIds(),
            request.aspectDefNames()
        );

        AspectQueryResponse response = new AspectQueryResponse(
            catalogId,
            results
        );

        return ResponseEntity.ok(response);
    }
}
