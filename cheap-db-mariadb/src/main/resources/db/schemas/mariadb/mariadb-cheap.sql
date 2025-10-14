-- MariaDB DDL for Cheap Data Model
-- Represents the core Cheap model: Catalog, Hierarchy, Entity, Aspect, Property
-- Simplified schema: CatalogDef and HierarchyDef are informational only, not persisted

-- ========== CORE CHEAP ELEMENT TABLES ==========

-- Entity: Represents entities with only global ID (conceptual objects)
CREATE TABLE entity (
    entity_id CHAR(36) PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AspectDef: First-class entity defining aspect structure and metadata
CREATE TABLE aspect_def (
    aspect_def_id CHAR(36) PRIMARY KEY,
    name TEXT NOT NULL,
    hash_version BIGINT, -- Hash-based version (implicit, based on content)
    is_readable BOOLEAN NOT NULL DEFAULT true,
    is_writable BOOLEAN NOT NULL DEFAULT true,
    can_add_properties BOOLEAN NOT NULL DEFAULT false,
    can_remove_properties BOOLEAN NOT NULL DEFAULT false,
    UNIQUE KEY unique_aspect_def_name (name(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PropertyDef: Defines property structure within AspectDefs
-- No global ID - identified by name within AspectDef
CREATE TABLE property_def (
    aspect_def_id CHAR(36) NOT NULL,
    name TEXT NOT NULL,
    property_type VARCHAR(3) NOT NULL CHECK (property_type IN (
        'INT', 'FLT', 'BLN', 'STR', 'TXT', 'BGI', 'BGF',
        'DAT', 'URI', 'UID', 'CLB', 'BLB'
    )),
    default_value TEXT,
    has_default_value BOOLEAN NOT NULL DEFAULT false,
    is_readable BOOLEAN NOT NULL DEFAULT true,
    is_writable BOOLEAN NOT NULL DEFAULT true,
    is_nullable BOOLEAN NOT NULL DEFAULT true,
    is_removable BOOLEAN NOT NULL DEFAULT false,
    is_multivalued BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (aspect_def_id, name(255)),
    CONSTRAINT fk_property_def_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalog: Extends Entity, represents catalog instances
CREATE TABLE catalog (
    catalog_id CHAR(36) PRIMARY KEY,
    species VARCHAR(10) NOT NULL CHECK (species IN ('SOURCE', 'SINK', 'MIRROR', 'CACHE', 'CLONE', 'FORK')),
    uri TEXT,
    upstream_catalog_id CHAR(36),
    version_number BIGINT NOT NULL DEFAULT 0 -- Integer version (manual)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Link table: Catalog to AspectDef (many-to-many)
CREATE TABLE catalog_aspect_def (
    catalog_id CHAR(36) NOT NULL,
    aspect_def_id CHAR(36) NOT NULL,
    PRIMARY KEY (catalog_id, aspect_def_id),
    CONSTRAINT fk_catalog_aspect_def_catalog FOREIGN KEY (catalog_id)
        REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    CONSTRAINT fk_catalog_aspect_def_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Hierarchy: Hierarchy instances within catalogs
-- No global ID - identified by name within catalog
-- Hierarchy type and name are stored directly in the hierarchy
CREATE TABLE hierarchy (
    catalog_id CHAR(36) NOT NULL,
    name TEXT NOT NULL,
    hierarchy_type VARCHAR(2) NOT NULL CHECK (hierarchy_type IN ('EL', 'ES', 'ED', 'ET', 'AM')),
    version_number BIGINT NOT NULL DEFAULT 0, -- Integer version (manual)
    PRIMARY KEY (catalog_id, name(255)),
    CONSTRAINT fk_hierarchy_catalog FOREIGN KEY (catalog_id)
        REFERENCES catalog(catalog_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Aspect: Aspect instances attached to entities
-- No global ID - identified by entity_id + aspect_def + catalog combination
CREATE TABLE aspect (
    entity_id CHAR(36) NOT NULL,
    aspect_def_id CHAR(36) NOT NULL,
    catalog_id CHAR(36) NOT NULL,
    hierarchy_name TEXT NOT NULL,
    PRIMARY KEY (entity_id, aspect_def_id, catalog_id),
    CONSTRAINT fk_aspect_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE,
    CONSTRAINT fk_aspect_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    CONSTRAINT fk_aspect_catalog FOREIGN KEY (catalog_id)
        REFERENCES catalog(catalog_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== PROPERTY VALUE STORAGE ==========

-- Generic property value table: One row per property value
-- No global ID - identified by aspect + property_name + value_index combination
-- For multivalued properties, one row is inserted per value with sequential value_index
-- For single-valued properties, value_index is always 0
-- Note that this table is insanely inefficient; it's expected aspects will
-- mainly be stored in dedicated tables.
CREATE TABLE property_value (
    entity_id CHAR(36) NOT NULL,
    aspect_def_id CHAR(36) NOT NULL,
    catalog_id CHAR(36) NOT NULL,
    property_name TEXT NOT NULL,
    value_index INTEGER NOT NULL DEFAULT 0,

    -- Value storage columns - use value_text for all types except BLOB (which uses value_binary)
    value_text TEXT,
    value_binary LONGBLOB,

    PRIMARY KEY (entity_id, aspect_def_id, catalog_id, property_name(255), value_index),
    CONSTRAINT fk_property_value_aspect FOREIGN KEY (entity_id, aspect_def_id, catalog_id)
        REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== HIERARCHY CONTENT TABLES ==========

-- Entity List Hierarchy: Ordered list with possible duplicates
CREATE TABLE hierarchy_entity_list (
    catalog_id CHAR(36) NOT NULL,
    hierarchy_name TEXT NOT NULL,
    entity_id CHAR(36) NOT NULL,
    list_order INTEGER NOT NULL,
    PRIMARY KEY (catalog_id, hierarchy_name(255), list_order),
    CONSTRAINT fk_hierarchy_entity_list_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entity Set Hierarchy: Unique entities (possibly ordered)
CREATE TABLE hierarchy_entity_set (
    catalog_id CHAR(36) NOT NULL,
    hierarchy_name TEXT NOT NULL,
    entity_id CHAR(36) NOT NULL,
    set_order INTEGER,
    PRIMARY KEY (catalog_id, hierarchy_name(255), entity_id),
    CONSTRAINT fk_hierarchy_entity_set_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entity Directory Hierarchy: String-to-entity mapping
CREATE TABLE hierarchy_entity_directory (
    catalog_id CHAR(36) NOT NULL,
    hierarchy_name TEXT NOT NULL,
    entity_key TEXT NOT NULL,
    entity_id CHAR(36) NOT NULL,
    dir_order INTEGER NOT NULL,
    PRIMARY KEY (catalog_id, hierarchy_name(255), entity_key(255)),
    CONSTRAINT fk_hierarchy_entity_directory_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entity Tree Hierarchy: Tree structure with named nodes
CREATE TABLE hierarchy_entity_tree_node (
    node_id CHAR(36) PRIMARY KEY,
    catalog_id CHAR(36) NOT NULL,
    hierarchy_name TEXT NOT NULL,
    parent_node_id CHAR(36),
    node_key TEXT,
    entity_id CHAR(36),
    node_path TEXT, -- Computed path for efficient queries
    tree_order INTEGER NOT NULL,
    CONSTRAINT fk_hierarchy_entity_tree_parent FOREIGN KEY (parent_node_id)
        REFERENCES hierarchy_entity_tree_node(node_id) ON DELETE CASCADE,
    CONSTRAINT fk_hierarchy_entity_tree_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE,
    UNIQUE KEY unique_tree_node (catalog_id, hierarchy_name(255), parent_node_id, node_key(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Aspect Map Hierarchy: Entity-to-aspect mapping for single aspect type
CREATE TABLE hierarchy_aspect_map (
    catalog_id CHAR(36) NOT NULL,
    hierarchy_name TEXT NOT NULL,
    entity_id CHAR(36) NOT NULL,
    aspect_def_id CHAR(36) NOT NULL,
    map_order INTEGER NOT NULL,
    PRIMARY KEY (catalog_id, hierarchy_name(255), entity_id),
    CONSTRAINT fk_hierarchy_aspect_map_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id),
    CONSTRAINT fk_hierarchy_aspect_map_aspect FOREIGN KEY (entity_id, aspect_def_id, catalog_id)
        REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== INDEXES FOR PERFORMANCE ==========

-- AspectDef indexes
CREATE INDEX idx_aspect_def_name ON aspect_def(name(255));

-- PropertyDef indexes
CREATE INDEX idx_property_def_aspect_def_id ON property_def(aspect_def_id);
CREATE INDEX idx_property_def_name ON property_def(name(255));

-- Catalog indexes
CREATE INDEX idx_catalog_species ON catalog(species);
CREATE INDEX idx_catalog_upstream ON catalog(upstream_catalog_id);

-- Hierarchy indexes
CREATE INDEX idx_hierarchy_catalog_id ON hierarchy(catalog_id);
CREATE INDEX idx_hierarchy_name ON hierarchy(name(255));
CREATE INDEX idx_hierarchy_type ON hierarchy(hierarchy_type);

-- Aspect indexes
CREATE INDEX idx_aspect_entity_id ON aspect(entity_id);
CREATE INDEX idx_aspect_def_id ON aspect(aspect_def_id);
CREATE INDEX idx_aspect_catalog_id ON aspect(catalog_id);
CREATE INDEX idx_aspect_hierarchy_name ON aspect(hierarchy_name(255));

-- Property value indexes
CREATE INDEX idx_property_value_entity_id ON property_value(entity_id);
CREATE INDEX idx_property_value_aspect_def_id ON property_value(aspect_def_id);
CREATE INDEX idx_property_value_catalog_id ON property_value(catalog_id);
CREATE INDEX idx_property_value_name ON property_value(property_name(255));

-- Hierarchy content indexes
CREATE INDEX idx_hierarchy_entity_list_catalog_id ON hierarchy_entity_list(catalog_id);
CREATE INDEX idx_hierarchy_entity_list_name ON hierarchy_entity_list(hierarchy_name(255));
CREATE INDEX idx_hierarchy_entity_list_entity_id ON hierarchy_entity_list(entity_id);
CREATE INDEX idx_hierarchy_entity_list_order ON hierarchy_entity_list(list_order);

CREATE INDEX idx_hierarchy_entity_set_catalog_id ON hierarchy_entity_set(catalog_id);
CREATE INDEX idx_hierarchy_entity_set_name ON hierarchy_entity_set(hierarchy_name(255));
CREATE INDEX idx_hierarchy_entity_set_entity_id ON hierarchy_entity_set(entity_id);

CREATE INDEX idx_hierarchy_entity_directory_catalog_id ON hierarchy_entity_directory(catalog_id);
CREATE INDEX idx_hierarchy_entity_directory_name ON hierarchy_entity_directory(hierarchy_name(255));
CREATE INDEX idx_hierarchy_entity_directory_entity_id ON hierarchy_entity_directory(entity_id);
CREATE INDEX idx_hierarchy_entity_directory_key ON hierarchy_entity_directory(entity_key(255));

CREATE INDEX idx_hierarchy_entity_tree_catalog_id ON hierarchy_entity_tree_node(catalog_id);
CREATE INDEX idx_hierarchy_entity_tree_name ON hierarchy_entity_tree_node(hierarchy_name(255));
CREATE INDEX idx_hierarchy_entity_tree_parent_id ON hierarchy_entity_tree_node(parent_node_id);
CREATE INDEX idx_hierarchy_entity_tree_entity_id ON hierarchy_entity_tree_node(entity_id);
CREATE INDEX idx_hierarchy_entity_tree_path ON hierarchy_entity_tree_node(node_path(255));

CREATE INDEX idx_hierarchy_aspect_map_catalog_id ON hierarchy_aspect_map(catalog_id);
CREATE INDEX idx_hierarchy_aspect_map_name ON hierarchy_aspect_map(hierarchy_name(255));
CREATE INDEX idx_hierarchy_aspect_map_entity_id ON hierarchy_aspect_map(entity_id);
CREATE INDEX idx_hierarchy_aspect_map_aspect_def_id ON hierarchy_aspect_map(aspect_def_id);
