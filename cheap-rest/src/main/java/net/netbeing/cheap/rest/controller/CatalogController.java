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

import net.netbeing.cheap.rest.dto.CreateCatalogRequest;
import net.netbeing.cheap.rest.dto.CreateCatalogResponse;
import net.netbeing.cheap.rest.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Catalog operations.
 */
@RestController
@RequestMapping("/api/catalogs")
public class CatalogController
{
    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService)
    {
        this.catalogService = catalogService;
    }

    /**
     * Creates a new catalog.
     *
     * @param request the catalog creation request
     * @return the created catalog ID and metadata
     */
    @PostMapping
    public ResponseEntity<CreateCatalogResponse> createCatalog(@RequestBody CreateCatalogRequest request)
    {
        logger.info("Received request to create catalog with species: {}", request.species());

        UUID catalogId = catalogService.createCatalog(
            request.catalogDef(),
            request.species(),
            request.upstream(),
            request.uri()
        );

        CreateCatalogResponse response = new CreateCatalogResponse(
            catalogId,
            request.uri(),
            "Catalog created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
