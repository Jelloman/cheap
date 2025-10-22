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

package net.netbeing.cheap.db;

import net.netbeing.cheap.impl.basic.CheapFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Factory class for creating common test data used across cheap-db test classes.
 * This class helps reduce duplication in test setup by providing reusable methods
 * for creating and populating test Cheap elements and database tables.
 */
public class CheapTestFactory
{
    private final CheapFactory factory;

    /**
     * Creates a new CheapTestFactory with a default CheapFactory.
     */
    public CheapTestFactory()
    {
        this.factory = new CheapFactory();
    }

    /**
     * Creates a new CheapTestFactory with the specified CheapFactory.
     *
     * @param factory the CheapFactory to use
     */
    public CheapTestFactory(CheapFactory factory)
    {
        this.factory = factory;
    }

    /**
     * Gets the underlying CheapFactory.
     *
     * @return the CheapFactory instance
     */
    public CheapFactory getFactory()
    {
        return factory;
    }

    // ===== Database Schema Test Data Creation Methods =====

    /**
     * Creates a complete test entity in the database with an aspect, property,
     * catalog, hierarchy, and property value.
     *
     * @param conn the database connection
     * @return the EntityIds containing all created IDs
     * @throws SQLException if database operation fails
     */
    public EntityIds createTestEntity(Connection conn) throws SQLException
    {
        return createTestEntity(conn, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    /**
     * Creates a complete test entity in the database with specified IDs.
     *
     * @param conn the database connection
     * @param entityId the entity ID to use
     * @param aspectDefId the aspect definition ID to use
     * @param catalogId the catalog ID to use
     * @return the EntityIds containing all created IDs
     * @throws SQLException if database operation fails
     */
    public EntityIds createTestEntity(Connection conn, UUID entityId, UUID aspectDefId, UUID catalogId) throws SQLException
    {
        insertEntity(conn, entityId);
        insertAspectDef(conn, aspectDefId, "test_aspect", 123L);
        insertPropertyDef(conn, aspectDefId, "test_prop");
        insertCatalog(conn, catalogId);
        insertCatalogAspectDef(conn, catalogId, aspectDefId);
        insertHierarchy(conn, catalogId, "test_hierarchy", "EL", 1L);
        insertAspect(conn, entityId, aspectDefId, catalogId, "test_hierarchy");
        insertPropertyValue(conn, entityId, aspectDefId, catalogId, "test_prop", "test_value");

        return new EntityIds(entityId, aspectDefId, catalogId, "test_hierarchy");
    }

    /**
     * Populates all hierarchy types in the database with test data.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID to use
     * @return the EntityIds containing all created IDs
     * @throws SQLException if database operation fails
     */
    public EntityIds populateAllHierarchyTypes(Connection conn, UUID catalogId) throws SQLException
    {
        UUID entityId = UUID.randomUUID();
        UUID aspectDefId = UUID.randomUUID();

        insertEntity(conn, entityId);
        insertAspectDef(conn, aspectDefId, "test_aspect", 123L);
        insertPropertyDef(conn, aspectDefId, "test_prop");
        insertCatalog(conn, catalogId);
        insertCatalogAspectDef(conn, catalogId, aspectDefId);

        // Insert into hierarchy_entity_list
        insertHierarchy(conn, catalogId, "test_hierarchy", "EL", 1L);
        insertAspect(conn, entityId, aspectDefId, catalogId, "test_hierarchy");
        insertPropertyValue(conn, entityId, aspectDefId, catalogId, "test_prop", "test_value");
        insertHierarchyEntityList(conn, catalogId, "test_hierarchy", entityId, 0);

        // Insert into hierarchy_entity_set
        UUID entityId2 = UUID.randomUUID();
        insertEntity(conn, entityId2);
        insertHierarchy(conn, catalogId, "test_set", "ES", 1L);
        insertHierarchyEntitySet(conn, catalogId, "test_set", entityId2, 0);

        // Insert into hierarchy_entity_directory
        insertHierarchy(conn, catalogId, "test_dir", "ED", 1L);
        insertHierarchyEntityDirectory(conn, catalogId, "test_dir", "key1", entityId, 0);

        // Insert into hierarchy_entity_tree_node
        insertHierarchy(conn, catalogId, "test_tree", "ET", 1L);
        UUID nodeId = UUID.randomUUID();
        insertHierarchyEntityTreeNode(conn, nodeId, catalogId, "test_tree", null, "", entityId, "", 0);

        // Insert into hierarchy_aspect_map
        insertHierarchy(conn, catalogId, "test_aspect", "AM", 1L);
        insertHierarchyAspectMap(conn, catalogId, "test_aspect", entityId, aspectDefId, 0);

        return new EntityIds(entityId, aspectDefId, catalogId, "test_hierarchy");
    }

    // ===== Individual Table Insert Methods =====

    /**
     * Inserts an entity into the entity table.
     *
     * @param conn the database connection
     * @param entityId the entity ID (as UUID or String depending on database)
     * @throws SQLException if database operation fails
     */
    public void insertEntity(Connection conn, Object entityId) throws SQLException
    {
        executeUpdate(conn, "INSERT INTO entity (entity_id) VALUES (?)", entityId);
    }

    /**
     * Inserts an aspect definition into the aspect_def table.
     *
     * @param conn the database connection
     * @param aspectDefId the aspect definition ID
     * @param name the aspect name
     * @param hashVersion the hash version
     * @throws SQLException if database operation fails
     */
    public void insertAspectDef(Connection conn, Object aspectDefId, String name, Long hashVersion) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO aspect_def (aspect_def_id, name, hash_version, is_readable, is_writable, can_add_properties, can_remove_properties) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            aspectDefId, name, hashVersion, true, true, false, false);
    }

    /**
     * Inserts a property definition into the property_def table.
     *
     * @param conn the database connection
     * @param aspectDefId the aspect definition ID
     * @param name the property name
     * @throws SQLException if database operation fails
     */
    public void insertPropertyDef(Connection conn, Object aspectDefId, String name) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO property_def (aspect_def_id, name, property_type, default_value, has_default_value, " +
                "is_readable, is_writable, is_nullable, is_removable, is_multivalued) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            aspectDefId, name, "STR", null, false, true, true, true, false, false);
    }

    /**
     * Inserts a catalog into the catalog table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @throws SQLException if database operation fails
     */
    public void insertCatalog(Connection conn, Object catalogId) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO catalog (catalog_id, species, uri, upstream_catalog_id, version_number) VALUES (?, ?, ?, ?, ?)",
            catalogId, "SINK", null, null, 1L);
    }

    /**
     * Inserts a catalog-aspect definition relationship into the catalog_aspect_def table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @param aspectDefId the aspect definition ID
     * @throws SQLException if database operation fails
     */
    public void insertCatalogAspectDef(Connection conn, Object catalogId, Object aspectDefId) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO catalog_aspect_def (catalog_id, aspect_def_id) VALUES (?, ?)",
            catalogId, aspectDefId);
    }

    /**
     * Inserts a hierarchy into the hierarchy table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @param name the hierarchy name
     * @param hierarchyType the hierarchy type
     * @param versionNumber the version number
     * @throws SQLException if database operation fails
     */
    public void insertHierarchy(Connection conn, Object catalogId, String name, String hierarchyType, Long versionNumber) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) VALUES (?, ?, ?, ?)",
            catalogId, name, hierarchyType, versionNumber);
    }

    /**
     * Inserts an aspect into the aspect table.
     *
     * @param conn the database connection
     * @param entityId the entity ID
     * @param aspectDefId the aspect definition ID
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @throws SQLException if database operation fails
     */
    public void insertAspect(Connection conn, Object entityId, Object aspectDefId, Object catalogId, String hierarchyName) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_name) VALUES (?, ?, ?, ?)",
            entityId, aspectDefId, catalogId, hierarchyName);
    }

    /**
     * Inserts a property value into the property_value table.
     *
     * @param conn the database connection
     * @param entityId the entity ID
     * @param aspectDefId the aspect definition ID
     * @param catalogId the catalog ID
     * @param propertyName the property name
     * @param valueText the text value
     * @throws SQLException if database operation fails
     */
    public void insertPropertyValue(Connection conn, Object entityId, Object aspectDefId, Object catalogId,
                                     String propertyName, String valueText) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO property_value (entity_id, aspect_def_id, catalog_id, property_name, value_text, value_binary) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            entityId, aspectDefId, catalogId, propertyName, valueText, null);
    }

    /**
     * Inserts an entry into the hierarchy_entity_list table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityId the entity ID
     * @param listOrder the list order
     * @throws SQLException if database operation fails
     */
    public void insertHierarchyEntityList(Connection conn, Object catalogId, String hierarchyName, Object entityId, int listOrder) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO hierarchy_entity_list (catalog_id, hierarchy_name, entity_id, list_order) VALUES (?, ?, ?, ?)",
            catalogId, hierarchyName, entityId, listOrder);
    }

    /**
     * Inserts an entry into the hierarchy_entity_set table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityId the entity ID
     * @param setOrder the set order
     * @throws SQLException if database operation fails
     */
    public void insertHierarchyEntitySet(Connection conn, Object catalogId, String hierarchyName, Object entityId, int setOrder) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO hierarchy_entity_set (catalog_id, hierarchy_name, entity_id, set_order) VALUES (?, ?, ?, ?)",
            catalogId, hierarchyName, entityId, setOrder);
    }

    /**
     * Inserts an entry into the hierarchy_entity_directory table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityKey the entity key
     * @param entityId the entity ID
     * @param dirOrder the directory order
     * @throws SQLException if database operation fails
     */
    public void insertHierarchyEntityDirectory(Connection conn, Object catalogId, String hierarchyName,
                                                String entityKey, Object entityId, int dirOrder) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO hierarchy_entity_directory (catalog_id, hierarchy_name, entity_key, entity_id, dir_order) " +
                "VALUES (?, ?, ?, ?, ?)",
            catalogId, hierarchyName, entityKey, entityId, dirOrder);
    }

    /**
     * Inserts an entry into the hierarchy_entity_tree_node table.
     *
     * @param conn the database connection
     * @param nodeId the node ID
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param parentNodeId the parent node ID (can be null)
     * @param nodeKey the node key
     * @param entityId the entity ID
     * @param nodePath the node path
     * @param treeOrder the tree order
     * @throws SQLException if database operation fails
     */
    public void insertHierarchyEntityTreeNode(Connection conn, Object nodeId, Object catalogId, String hierarchyName,
                                               Object parentNodeId, String nodeKey, Object entityId,
                                               String nodePath, int treeOrder) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO hierarchy_entity_tree_node (node_id, catalog_id, hierarchy_name, parent_node_id, " +
                "node_key, entity_id, node_path, tree_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            nodeId, catalogId, hierarchyName, parentNodeId, nodeKey, entityId, nodePath, treeOrder);
    }

    /**
     * Inserts an entry into the hierarchy_aspect_map table.
     *
     * @param conn the database connection
     * @param catalogId the catalog ID
     * @param hierarchyName the hierarchy name
     * @param entityId the entity ID
     * @param aspectDefId the aspect definition ID
     * @param mapOrder the map order
     * @throws SQLException if database operation fails
     */
    public void insertHierarchyAspectMap(Connection conn, Object catalogId, String hierarchyName,
                                          Object entityId, Object aspectDefId, int mapOrder) throws SQLException
    {
        executeUpdate(conn,
            "INSERT INTO hierarchy_aspect_map (catalog_id, hierarchy_name, entity_id, aspect_def_id, map_order) " +
                "VALUES (?, ?, ?, ?, ?)",
            catalogId, hierarchyName, entityId, aspectDefId, mapOrder);
    }

    // ===== Helper Methods =====

    /**
     * Executes a SQL update statement with parameters.
     *
     * @param conn the database connection
     * @param sql the SQL statement
     * @param params the parameters to bind
     * @throws SQLException if database operation fails
     */
    private void executeUpdate(Connection conn, String sql, Object... params) throws SQLException
    {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        }
    }

    /**
     * Record class to hold entity-related IDs created during test setup.
     */
    public record EntityIds(UUID entityId, UUID aspectDefId, UUID catalogId, String hierarchyName)
    {
    }
}
