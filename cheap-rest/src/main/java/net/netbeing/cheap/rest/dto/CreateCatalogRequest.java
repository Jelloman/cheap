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

package net.netbeing.cheap.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;

import java.net.URI;
import java.util.UUID;

/**
 * Request DTO for creating a new catalog.
 */
public record CreateCatalogRequest(
    @JsonProperty("catalogDef") CatalogDef catalogDef,
    @JsonProperty("species") CatalogSpecies species,
    @JsonProperty("upstream") UUID upstream,
    @JsonProperty("uri") URI uri
)
{
}
