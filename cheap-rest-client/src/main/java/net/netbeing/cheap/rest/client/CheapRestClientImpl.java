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

package net.netbeing.cheap.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.rest.client.exception.CheapRestBadRequestException;
import net.netbeing.cheap.rest.client.exception.CheapRestClientException;
import net.netbeing.cheap.rest.client.exception.CheapRestNotFoundException;
import net.netbeing.cheap.rest.client.exception.CheapRestServerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of CheapRestClient using Spring WebClient.
 */
@SuppressWarnings("unused")
@Slf4j
public class CheapRestClientImpl implements CheapRestClient
{
    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;
    protected final CheapFactory factory;

    /**
     * Creates a new CheapRestClient with the specified base URL.
     *
     * @param baseUrl the base URL of the cheap-rest service (e.g., "http://localhost:8080")
     */
    public CheapRestClientImpl(@NotNull String baseUrl)
    {
        this.objectMapper = new ObjectMapper();
        this.factory = new CheapFactory();
        registerCheapModules();

        // Configure WebClient with our custom ObjectMapper
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
            })
            .build();

        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .exchangeStrategies(strategies)
            .build();
    }

    /**
     * Creates a new CheapRestClient with a custom WebClient.
     *
     * @param webClient the WebClient to use
     */
    public CheapRestClientImpl(@NotNull WebClient webClient)
    {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
        this.factory = new CheapFactory();
        registerCheapModules();
    }

    protected void registerCheapModules()
    {
        // Configure ObjectMapper with Cheap serializers and deserializers
        objectMapper.registerModule(CheapJacksonSerializer.createCheapModule());
        objectMapper.registerModule(CheapJacksonDeserializer.createCheapModule(factory));
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public CheapFactory getFactory()
    {
        return factory;
    }

    // ========== Catalog Operations ==========

    @Override
    public void registerAspectDef(@NotNull AspectDef aspectDef)
    {
        factory.registerAspectDef(aspectDef);
    }

    @Override
    public CreateCatalogResponse createCatalog(
        @NotNull CatalogDef catalogDef,
        @NotNull CatalogSpecies species,
        UUID upstream)
    {
        CreateCatalogRequest request = new CreateCatalogRequest(catalogDef, species, upstream);

        return webClient.post()
            .uri("/api/catalog")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CreateCatalogResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public CatalogListResponse listCatalogs(int page, int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalog")
                .queryParam("page", page)
                .queryParam("size", size)
                .build())
            .retrieve()
            .bodyToMono(CatalogListResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public CatalogDef getCatalogDef(@NotNull UUID catalogId)
    {
        return webClient.get()
            .uri("/api/catalog/{catalogId}", catalogId)
            .retrieve()
            .bodyToMono(CatalogDef.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== AspectDef Operations ==========

    @Override
    public CreateAspectDefResponse createAspectDef(
        @NotNull UUID catalogId,
        @NotNull AspectDef aspectDef)
    {
        CreateAspectDefResponse response = webClient.post()
            .uri("/api/catalog/{catalogId}/aspect-defs", catalogId)
            .bodyValue(aspectDef)
            .retrieve()
            .bodyToMono(CreateAspectDefResponse.class)
            .onErrorMap(this::mapException)
            .block();

        if (response != null && response.aspectDefId() != null) {
            registerAspectDef(aspectDef);
        }
        return response;
    }

    @Override
    public AspectDefListResponse listAspectDefs(@NotNull UUID catalogId, int page, int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalog/{catalogId}/aspect-defs")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(catalogId))
            .retrieve()
            .bodyToMono(AspectDefListResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public AspectDef getAspectDef(@NotNull UUID catalogId, @NotNull UUID aspectDefId)
    {
        return webClient.get()
            .uri("/api/catalog/{catalogId}/aspect-defs/{aspectDefId}", catalogId, aspectDefId)
            .retrieve()
            .bodyToMono(AspectDef.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public AspectDef getAspectDefByName(@NotNull UUID catalogId, @NotNull String aspectDefName)
    {
        return webClient.get()
            .uri("/api/catalog/{catalogId}/aspect-defs/{aspectDefName}", catalogId, aspectDefName)
            .retrieve()
            .bodyToMono(AspectDef.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== Aspect Operations ==========

    @Override
    public UpsertAspectsResponse upsertAspects(
        @NotNull UUID catalogId,
        @NotNull String aspectDefName,
        @NotNull Map<UUID, Map<String, Object>> aspects)
    {
        UpsertAspectsRequest request = new UpsertAspectsRequest(
            aspects.entrySet()
                .stream()
                .map(entry -> new UpsertAspectsRequest.AspectData(
                    entry.getKey(),
                    entry.getValue()
                ))
                .toList()
        );

        return webClient.post()
            .uri("/api/catalog/{catalogId}/aspects/{aspectDefName}", catalogId, aspectDefName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(UpsertAspectsResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public AspectQueryResponse queryAspects(
        @NotNull UUID catalogId,
        @NotNull Set<UUID> entityIds,
        @NotNull Set<String> aspectDefNames)
    {
        AspectQueryRequest request = new AspectQueryRequest(entityIds, aspectDefNames);

        return webClient.post()
            .uri("/api/catalog/{catalogId}/aspects/query", catalogId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AspectQueryResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== Hierarchy Operations ==========

    @Override
    public EntityListResponse getEntityList(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalog/{catalogId}/hierarchies/{hierarchyName}")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(catalogId, hierarchyName))
            .retrieve()
            .bodyToMono(EntityListResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public EntityDirectoryResponse getEntityDirectory(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName)
    {
        return webClient.get()
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}", catalogId, hierarchyName)
            .retrieve()
            .bodyToMono(EntityDirectoryResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public EntityTreeResponse getEntityTree(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName)
    {
        return webClient.get()
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}", catalogId, hierarchyName)
            .retrieve()
            .bodyToMono(EntityTreeResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public AspectMapResponse getAspectMap(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        int page,
        int size)
    {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/catalog/{catalogId}/hierarchies/{hierarchyName}")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(catalogId, hierarchyName))
            .retrieve()
            .bodyToMono(AspectMapResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== Hierarchy Mutation Operations ==========

    @Override
    public EntityIdsOperationResponse addEntityIds(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<UUID> entityIds)
    {
        AddEntityIdsRequest request = new AddEntityIdsRequest(entityIds);

        return webClient.post()
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(EntityIdsOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public EntityIdsOperationResponse removeEntityIds(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<UUID> entityIds)
    {
        RemoveEntityIdsRequest request = new RemoveEntityIdsRequest(entityIds);

        return webClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entities", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(EntityIdsOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public DirectoryOperationResponse addDirectoryEntries(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull Map<String, UUID> entries)
    {
        AddDirectoryEntriesRequest request = new AddDirectoryEntriesRequest(entries);

        return webClient.post()
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entries", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DirectoryOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public DirectoryOperationResponse removeDirectoryEntriesByNames(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<String> names)
    {
        RemoveDirectoryEntriesRequest request = new RemoveDirectoryEntriesRequest(names, null);

        return webClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entries", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DirectoryOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public DirectoryOperationResponse removeDirectoryEntriesByEntityIds(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<UUID> entityIds)
    {
        RemoveDirectoryEntriesRequest request = new RemoveDirectoryEntriesRequest(null, entityIds);

        return webClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/entries", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DirectoryOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public TreeOperationResponse addTreeNodes(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        String parentPath,
        @NotNull Map<String, UUID> nodes)
    {
        AddTreeNodesRequest request = new AddTreeNodesRequest(parentPath, nodes);

        return webClient.post()
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/nodes", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TreeOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    @Override
    public TreeOperationResponse removeTreeNodes(
        @NotNull UUID catalogId,
        @NotNull String hierarchyName,
        @NotNull List<String> paths)
    {
        RemoveTreeNodesRequest request = new RemoveTreeNodesRequest(paths);

        return webClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/{catalogId}/hierarchies/{hierarchyName}/nodes", catalogId, hierarchyName)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TreeOperationResponse.class)
            .onErrorMap(this::mapException)
            .block();
    }

    // ========== Error Handling ==========

    private Throwable mapException(Throwable throwable)
    {
        if (throwable instanceof WebClientResponseException ex) {
            return switch (ex.getStatusCode().value()) {
                case 400 -> new CheapRestBadRequestException("Bad request: " + ex.getMessage(), ex);
                case 404 -> new CheapRestNotFoundException("Resource not found: " + ex.getMessage(), ex);
                case 500, 503 -> new CheapRestServerException("Server error: " + ex.getMessage(), ex);
                default ->
                    new CheapRestClientException("HTTP error " + ex.getStatusCode() + ": " + ex.getMessage(), ex);
            };
        }

        return new CheapRestClientException("Request failed: " + throwable.getMessage(), throwable);
    }
}
