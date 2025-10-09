-- SQLite DDL for Cheap Data Model
-- Represents the core Cheap model: Catalog, Hierarchy, Entity, Aspect, Property
-- Simplified schema: CatalogDef and HierarchyDef are informational only, not persisted

-- SQLite Note: UUIDs are stored as TEXT (36-character strings)
-- UUID generation must be handled by application code

-- Enable foreign key constraints (must be set per connection in SQLite)
PRAGMA foreign_keys = ON;

-- ========== CORE CHEAP ELEMENT TABLES ==========

-- Entity: Represents entities with only global ID (conceptual objects)
CREATE TABLE entity (
    entity_id TEXT PRIMARY KEY
);

-- AspectDef: First-class entity defining aspect structure and metadata
CREATE TABLE aspect_def (
    aspect_def_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    hash_version TEXT, -- Hash-based version (implicit, based on content)
    is_readable INTEGER NOT NULL DEFAULT 1 CHECK (is_readable IN (0, 1)),
    is_writable INTEGER NOT NULL DEFAULT 1 CHECK (is_writable IN (0, 1)),
    can_add_properties INTEGER NOT NULL DEFAULT 0 CHECK (can_add_properties IN (0, 1)),
    can_remove_properties INTEGER NOT NULL DEFAULT 0 CHECK (can_remove_properties IN (0, 1))
);

-- PropertyDef: Defines property structure within AspectDefs
-- No global ID - identified by name within AspectDef
CREATE TABLE property_def (
    aspect_def_id TEXT NOT NULL REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    property_type TEXT NOT NULL CHECK (property_type IN (
        'INT', 'FLT', 'BLN', 'STR', 'TXT', 'BGI', 'BGF',
        'DAT', 'URI', 'UID', 'CLB', 'BLB'
    )),
    default_value TEXT,
    has_default_value INTEGER NOT NULL DEFAULT 0 CHECK (has_default_value IN (0, 1)),
    is_readable INTEGER NOT NULL DEFAULT 1 CHECK (is_readable IN (0, 1)),
    is_writable INTEGER NOT NULL DEFAULT 1 CHECK (is_writable IN (0, 1)),
    is_nullable INTEGER NOT NULL DEFAULT 1 CHECK (is_nullable IN (0, 1)),
    is_removable INTEGER NOT NULL DEFAULT 0 CHECK (is_removable IN (0, 1)),
    is_multivalued INTEGER NOT NULL DEFAULT 0 CHECK (is_multivalued IN (0, 1)),
    PRIMARY KEY (aspect_def_id, name)
);

-- Catalog: Extends Entity, represents catalog instances
CREATE TABLE catalog (
    catalog_id TEXT PRIMARY KEY,
    species TEXT NOT NULL CHECK (species IN ('SOURCE', 'SINK', 'MIRROR', 'CACHE', 'CLONE', 'FORK')),
    uri TEXT,
    upstream_catalog_id TEXT,
    version_number INTEGER NOT NULL DEFAULT 0 -- Integer version (manual)
);

-- Link table: Catalog to AspectDef (many-to-many)
CREATE TABLE catalog_aspect_def (
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    aspect_def_id TEXT NOT NULL REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    PRIMARY KEY (catalog_id, aspect_def_id)
);

-- Hierarchy: Hierarchy instances within catalogs
-- No global ID - identified by name within catalog
-- Hierarchy type and name are stored directly in the hierarchy
CREATE TABLE hierarchy (
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    hierarchy_type TEXT NOT NULL CHECK (hierarchy_type IN ('EL', 'ES', 'ED', 'ET', 'AM')),
    version_number INTEGER NOT NULL DEFAULT 0, -- Integer version (manual)
    PRIMARY KEY (catalog_id, name)
);

-- Aspect: Aspect instances attached to entities
-- No global ID - identified by entity_id + aspect_def + catalog combination
CREATE TABLE aspect (
    entity_id TEXT NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    aspect_def_id TEXT NOT NULL REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name TEXT NOT NULL,
    PRIMARY KEY (entity_id, aspect_def_id, catalog_id),
    FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- ========== PROPERTY VALUE STORAGE ==========

-- Generic property value table: One row per property value
-- No global ID - identified by aspect + property_name + value_index combination
-- For multivalued properties, one row is inserted per value with sequential value_index
-- For single-valued properties, value_index is always 0
-- Note that this table is insanely inefficient; it's expected aspects will
-- mainly be stored in dedicated tables.
CREATE TABLE property_value (
    entity_id TEXT NOT NULL,
    aspect_def_id TEXT NOT NULL,
    catalog_id TEXT NOT NULL,
    property_name TEXT NOT NULL,
    value_index INTEGER NOT NULL DEFAULT 0,

    -- Value storage columns - use value_text for all types except BLOB (which uses value_binary)
    -- Storage for all property types except BLOB (INT, FLT, BLN, STR, TXT, BGI, BGF, DAT, URI, UID, CLB)
    value_text TEXT,
    -- Storage for BLB type only
    value_binary BLOB,

    PRIMARY KEY (entity_id, aspect_def_id, catalog_id, property_name, value_index),
    FOREIGN KEY (entity_id, aspect_def_id, catalog_id) REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE,
    FOREIGN KEY (aspect_def_id, property_name) REFERENCES property_def(aspect_def_id, name)
);

-- ========== HIERARCHY CONTENT TABLES ==========

-- Entity List Hierarchy: Ordered list with possible duplicates
CREATE TABLE hierarchy_entity_list (
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name TEXT NOT NULL,
    entity_id TEXT NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    PRIMARY KEY (catalog_id, hierarchy_name, list_order),
    FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- Entity Set Hierarchy: Unique entities (possibly ordered)
CREATE TABLE hierarchy_entity_set (
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name TEXT NOT NULL,
    entity_id TEXT NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    set_order INTEGER,
    PRIMARY KEY (catalog_id, hierarchy_name, entity_id),
    FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- Entity Directory Hierarchy: String-to-entity mapping
CREATE TABLE hierarchy_entity_directory (
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name TEXT NOT NULL,
    entity_key TEXT NOT NULL,
    entity_id TEXT NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    dir_order INTEGER NOT NULL,
    PRIMARY KEY (catalog_id, hierarchy_name, entity_key),
    FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- Entity Tree Hierarchy: Tree structure with named nodes
CREATE TABLE hierarchy_entity_tree_node (
    node_id TEXT PRIMARY KEY,
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name TEXT NOT NULL,
    parent_node_id TEXT REFERENCES hierarchy_entity_tree_node(node_id) ON DELETE CASCADE,
    node_key TEXT,
    entity_id TEXT REFERENCES entity(entity_id) ON DELETE CASCADE,
    node_path TEXT, -- Computed path for efficient queries
    tree_order INTEGER NOT NULL,
    FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE,
    UNIQUE(catalog_id, hierarchy_name, parent_node_id, node_key)
);

-- Aspect Map Hierarchy: Entity-to-aspect mapping for single aspect type
CREATE TABLE hierarchy_aspect_map (
    catalog_id TEXT NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name TEXT NOT NULL,
    entity_id TEXT NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    aspect_def_id TEXT NOT NULL REFERENCES aspect_def(aspect_def_id),
    map_order INTEGER NOT NULL,
    PRIMARY KEY (catalog_id, hierarchy_name, entity_id),
    FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE,
    FOREIGN KEY (entity_id, aspect_def_id, catalog_id) REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE
);

-- ========== INDEXES FOR PERFORMANCE ==========

-- AspectDef indexes
CREATE INDEX idx_aspect_def_name ON aspect_def(name);

-- PropertyDef indexes
CREATE INDEX idx_property_def_aspect_def_id ON property_def(aspect_def_id);
CREATE INDEX idx_property_def_name ON property_def(name);

-- Catalog indexes
CREATE INDEX idx_catalog_species ON catalog(species);
CREATE INDEX idx_catalog_upstream ON catalog(upstream_catalog_id);

-- Hierarchy indexes
CREATE INDEX idx_hierarchy_catalog_id ON hierarchy(catalog_id);
CREATE INDEX idx_hierarchy_name ON hierarchy(name);
CREATE INDEX idx_hierarchy_type ON hierarchy(hierarchy_type);

-- Aspect indexes
CREATE INDEX idx_aspect_entity_id ON aspect(entity_id);
CREATE INDEX idx_aspect_def_id ON aspect(aspect_def_id);
CREATE INDEX idx_aspect_catalog_id ON aspect(catalog_id);
CREATE INDEX idx_aspect_hierarchy_name ON aspect(hierarchy_name);

-- Property value indexes
CREATE INDEX idx_property_value_entity_id ON property_value(entity_id);
CREATE INDEX idx_property_value_aspect_def_id ON property_value(aspect_def_id);
CREATE INDEX idx_property_value_catalog_id ON property_value(catalog_id);
CREATE INDEX idx_property_value_name ON property_value(property_name);

-- Hierarchy content indexes
CREATE INDEX idx_hierarchy_entity_list_catalog_id ON hierarchy_entity_list(catalog_id);
CREATE INDEX idx_hierarchy_entity_list_name ON hierarchy_entity_list(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_list_entity_id ON hierarchy_entity_list(entity_id);
CREATE INDEX idx_hierarchy_entity_list_order ON hierarchy_entity_list(list_order);

CREATE INDEX idx_hierarchy_entity_set_catalog_id ON hierarchy_entity_set(catalog_id);
CREATE INDEX idx_hierarchy_entity_set_name ON hierarchy_entity_set(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_set_entity_id ON hierarchy_entity_set(entity_id);

CREATE INDEX idx_hierarchy_entity_directory_catalog_id ON hierarchy_entity_directory(catalog_id);
CREATE INDEX idx_hierarchy_entity_directory_name ON hierarchy_entity_directory(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_directory_entity_id ON hierarchy_entity_directory(entity_id);
CREATE INDEX idx_hierarchy_entity_directory_key ON hierarchy_entity_directory(entity_key);

CREATE INDEX idx_hierarchy_entity_tree_catalog_id ON hierarchy_entity_tree_node(catalog_id);
CREATE INDEX idx_hierarchy_entity_tree_name ON hierarchy_entity_tree_node(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_tree_parent_id ON hierarchy_entity_tree_node(parent_node_id);
CREATE INDEX idx_hierarchy_entity_tree_entity_id ON hierarchy_entity_tree_node(entity_id);
CREATE INDEX idx_hierarchy_entity_tree_path ON hierarchy_entity_tree_node(node_path);

CREATE INDEX idx_hierarchy_aspect_map_catalog_id ON hierarchy_aspect_map(catalog_id);
CREATE INDEX idx_hierarchy_aspect_map_name ON hierarchy_aspect_map(hierarchy_name);
CREATE INDEX idx_hierarchy_aspect_map_entity_id ON hierarchy_aspect_map(entity_id);
CREATE INDEX idx_hierarchy_aspect_map_aspect_def_id ON hierarchy_aspect_map(aspect_def_id);
