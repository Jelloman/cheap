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

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.rest.dto.CreateAspectDefResponse;
import net.netbeing.cheap.rest.service.AspectDefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for AspectDef operations.
 */
@RestController
@RequestMapping("/api/catalogs/{catalogId}/aspect-defs")
public class AspectDefController
{
    private static final Logger logger = LoggerFactory.getLogger(AspectDefController.class);

    private final AspectDefService aspectDefService;

    public AspectDefController(AspectDefService aspectDefService)
    {
        this.aspectDefService = aspectDefService;
    }

    /**
     * Creates a new AspectDef in a catalog.
     *
     * @param catalogId the catalog ID
     * @param aspectDef the AspectDef to create
     * @return the created AspectDef metadata
     */
    @PostMapping
    public ResponseEntity<CreateAspectDefResponse> createAspectDef(
        @PathVariable UUID catalogId,
        @RequestBody AspectDef aspectDef)
    {
        logger.info("Received request to create AspectDef {} in catalog {}", 
            aspectDef.name(), catalogId);

        AspectDef created = aspectDefService.createAspectDef(catalogId, aspectDef);

        CreateAspectDefResponse response = new CreateAspectDefResponse(
            created.globalId(),
            created.name(),
            "AspectDef created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
