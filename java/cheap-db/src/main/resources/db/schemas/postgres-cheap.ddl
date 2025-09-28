-- PostgreSQL DDL for Cheap Data Model
-- Represents the core Cheap model: Catalog, Hierarchy, Entity, Aspect, Property
-- This schema supports both definitions (metadata) and elements (data instances)

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ========== CORE DEFINITION TABLES ==========

-- AspectDef: First-class entity defining aspect structure and metadata
CREATE TABLE aspect_def (
    aspect_def_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    hash_version TEXT, -- Hash-based version (implicit, based on content)
    is_readable BOOLEAN NOT NULL DEFAULT true,
    is_writable BOOLEAN NOT NULL DEFAULT true,
    can_add_properties BOOLEAN NOT NULL DEFAULT false,
    can_remove_properties BOOLEAN NOT NULL DEFAULT false
);

-- PropertyDef: Defines property structure within AspectDefs
-- No global ID - identified by name within AspectDef
CREATE TABLE property_def (
    aspect_def_id UUID NOT NULL REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    property_type VARCHAR(10) NOT NULL CHECK (property_type IN (
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
    PRIMARY KEY (aspect_def_id, name)
);

-- CatalogDef: Defines catalog structure and characteristics
CREATE TABLE catalog_def (
    catalog_def_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    hash_version TEXT -- Hash-based version (implicit, based on content)
);

-- Note: HierarchyDef and owner tables will be created after core tables to avoid dependency issues

-- Link table: CatalogDef to AspectDef (many-to-many)
CREATE TABLE catalog_def_aspect_def (
    catalog_def_id UUID NOT NULL REFERENCES catalog_def(catalog_def_id) ON DELETE CASCADE,
    aspect_def_id UUID NOT NULL REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    PRIMARY KEY (catalog_def_id, aspect_def_id)
);

-- Note: Link table for CatalogDef to HierarchyDef will be created after HierarchyDef tables

-- ========== CORE ENTITY TABLES ==========

-- Entity: Represents entities with only global ID (conceptual objects)
CREATE TABLE entity (
    entity_id UUID PRIMARY KEY DEFAULT uuid_generate_v4()
);

-- Catalog: Extends Entity, represents catalog instances
CREATE TABLE catalog (
    catalog_id UUID PRIMARY KEY REFERENCES entity(entity_id) ON DELETE CASCADE,
    catalog_def_id UUID NOT NULL REFERENCES catalog_def(catalog_def_id),
    species VARCHAR(10) NOT NULL CHECK (species IN ('SOURCE', 'SINK', 'MIRROR', 'CACHE', 'CLONE', 'FORK')),
    uri TEXT,
    upstream_catalog_id UUID REFERENCES catalog(catalog_id),
    is_strict BOOLEAN NOT NULL DEFAULT false,
    version_number BIGINT NOT NULL DEFAULT 0 -- Integer version (manual)
);

-- Link table: Catalog to AspectDef (many-to-many for non-strict catalogs)
CREATE TABLE catalog_aspect_def (
    catalog_id UUID NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    aspect_def_id UUID NOT NULL REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    PRIMARY KEY (catalog_id, aspect_def_id)
);

-- Hierarchy: Hierarchy instances within catalogs
-- No global ID - identified by name within catalog
CREATE TABLE hierarchy (
    catalog_id UUID NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    hierarchy_def_owner_id UUID, -- Will be made NOT NULL and add FK later
    hierarchy_def_name VARCHAR(255),  -- Will be made NOT NULL and add FK later
    version_number BIGINT NOT NULL DEFAULT 0, -- Integer version (manual)
    PRIMARY KEY (catalog_id, name)
    -- Note: Foreign key to hierarchy_def will be added later
);

-- Aspect: Aspect instances attached to entities
-- No global ID - identified by entity_id + aspect_def + catalog combination
CREATE TABLE aspect (
    entity_id UUID NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    aspect_def_id UUID NOT NULL REFERENCES aspect_def(aspect_def_id),
    catalog_id UUID NOT NULL REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_catalog_id UUID NOT NULL,
    hierarchy_name VARCHAR(255) NOT NULL,
    is_transferable BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (entity_id, aspect_def_id, catalog_id),
    FOREIGN KEY (hierarchy_catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- ========== PROPERTY VALUE STORAGE ==========

-- Generic property value table: One row per property value
-- No global ID - identified by aspect + property_name combination
CREATE TABLE property_value (
    entity_id UUID NOT NULL,
    aspect_def_id UUID NOT NULL,
    catalog_id UUID NOT NULL,
    property_name VARCHAR(255) NOT NULL,

    -- Value storage columns for different types
    value_text TEXT,
    value_integer BIGINT,
    value_float DOUBLE PRECISION,
    value_boolean BOOLEAN,
    value_datetime TIMESTAMP WITH TIME ZONE,
    value_binary BYTEA,

    -- Metadata
    value_type VARCHAR(10) NOT NULL CHECK (value_type IN (
        'INT', 'FLT', 'BLN', 'STR', 'TXT', 'BGI', 'BGF',
        'DAT', 'URI', 'UID', 'CLB', 'BLB'
    )),
    is_null BOOLEAN NOT NULL DEFAULT false,

    PRIMARY KEY (entity_id, aspect_def_id, catalog_id, property_name),
    FOREIGN KEY (entity_id, aspect_def_id, catalog_id) REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE,
    FOREIGN KEY (aspect_def_id, property_name) REFERENCES property_def(aspect_def_id, name)
);

-- ========== HIERARCHY DEFINITION TABLES ==========

-- HierarchyDefOwnerType: Discriminator table for hierarchy definition ownership
CREATE TABLE hierarchy_def_owner_type (
    owner_type_code CHAR(1) PRIMARY KEY, -- 'C' for CatalogDef, 'H' for Hierarchy
    name VARCHAR(50) NOT NULL
);

-- Insert the two owner types
INSERT INTO hierarchy_def_owner_type (owner_type_code, name) VALUES
    ('C', 'CatalogDef'),
    ('H', 'Hierarchy');

-- HierarchyDefOwner: Supertype table that can reference either CatalogDef or Hierarchy
CREATE TABLE hierarchy_def_owner (
    owner_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_type_code CHAR(1) NOT NULL REFERENCES hierarchy_def_owner_type(owner_type_code),
    catalog_def_id UUID REFERENCES catalog_def(catalog_def_id) ON DELETE CASCADE,
    catalog_id UUID REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    hierarchy_name VARCHAR(255),
    -- Constraints to ensure only one owner type is set
    CONSTRAINT chk_catalogdef_owner CHECK (
        (owner_type_code = 'C' AND catalog_def_id IS NOT NULL AND catalog_id IS NULL AND hierarchy_name IS NULL) OR
        (owner_type_code = 'H' AND catalog_def_id IS NULL AND catalog_id IS NOT NULL AND hierarchy_name IS NOT NULL)
    )
    -- Note: Foreign key to hierarchy will be added later to avoid circular dependency
);

-- HierarchyDef: Defines hierarchy structure and metadata
-- No global ID - identified by name within owner
CREATE TABLE hierarchy_def (
    owner_id UUID NOT NULL REFERENCES hierarchy_def_owner(owner_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    hierarchy_type VARCHAR(2) NOT NULL CHECK (hierarchy_type IN ('EL', 'ES', 'ED', 'ET', 'AM')),
    is_modifiable BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (owner_id, name)
);

-- Link table: CatalogDef to HierarchyDef (many-to-many)
-- This is now primarily for HierarchyDefs owned by CatalogDefs
CREATE TABLE catalog_def_hierarchy_def (
    catalog_def_id UUID NOT NULL REFERENCES catalog_def(catalog_def_id) ON DELETE CASCADE,
    hierarchy_def_owner_id UUID NOT NULL,
    hierarchy_def_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (catalog_def_id, hierarchy_def_owner_id, hierarchy_def_name),
    FOREIGN KEY (hierarchy_def_owner_id, hierarchy_def_name) REFERENCES hierarchy_def(owner_id, name) ON DELETE CASCADE
);

-- ========== HIERARCHY CONTENT TABLES ==========

-- Entity List Hierarchy: Ordered list with possible duplicates
CREATE TABLE hierarchy_entity_list (
    hierarchy_catalog_id UUID NOT NULL,
    hierarchy_name VARCHAR(255) NOT NULL,
    entity_id UUID NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    PRIMARY KEY (hierarchy_catalog_id, hierarchy_name, list_order),
    FOREIGN KEY (hierarchy_catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- Entity Set Hierarchy: Unique entities (possibly ordered)
CREATE TABLE hierarchy_entity_set (
    hierarchy_catalog_id UUID NOT NULL,
    hierarchy_name VARCHAR(255) NOT NULL,
    entity_id UUID NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    set_order INTEGER,
    PRIMARY KEY (hierarchy_catalog_id, hierarchy_name, entity_id),
    FOREIGN KEY (hierarchy_catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- Entity Directory Hierarchy: String-to-entity mapping
CREATE TABLE hierarchy_entity_directory (
    hierarchy_catalog_id UUID NOT NULL,
    hierarchy_name VARCHAR(255) NOT NULL,
    entity_key VARCHAR(1000) NOT NULL,
    entity_id UUID NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    PRIMARY KEY (hierarchy_catalog_id, hierarchy_name, entity_key),
    FOREIGN KEY (hierarchy_catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE
);

-- Entity Tree Hierarchy: Tree structure with named nodes
CREATE TABLE hierarchy_entity_tree_node (
    node_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hierarchy_catalog_id UUID NOT NULL,
    hierarchy_name VARCHAR(255) NOT NULL,
    parent_node_id UUID REFERENCES hierarchy_entity_tree_node(node_id) ON DELETE CASCADE,
    node_key VARCHAR(1000),
    entity_id UUID REFERENCES entity(entity_id) ON DELETE CASCADE,
    node_path TEXT, -- Computed path for efficient queries
    FOREIGN KEY (hierarchy_catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE,
    UNIQUE(hierarchy_catalog_id, hierarchy_name, parent_node_id, node_key)
);

-- Aspect Map Hierarchy: Entity-to-aspect mapping for single aspect type
CREATE TABLE hierarchy_aspect_map (
    hierarchy_catalog_id UUID NOT NULL,
    hierarchy_name VARCHAR(255) NOT NULL,
    entity_id UUID NOT NULL REFERENCES entity(entity_id) ON DELETE CASCADE,
    aspect_def_id UUID NOT NULL REFERENCES aspect_def(aspect_def_id),
    catalog_id UUID NOT NULL,
    map_order INTEGER,
    PRIMARY KEY (hierarchy_catalog_id, hierarchy_name, entity_id),
    FOREIGN KEY (hierarchy_catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE,
    FOREIGN KEY (entity_id, aspect_def_id, catalog_id) REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE
);

-- ========== ADD DEFERRED FOREIGN KEY CONSTRAINTS ==========

-- Add foreign key constraint from hierarchy_def_owner to hierarchy (deferred to avoid circular dependency)
ALTER TABLE hierarchy_def_owner
ADD CONSTRAINT fk_hierarchy_owner FOREIGN KEY (catalog_id, hierarchy_name) REFERENCES hierarchy(catalog_id, name) ON DELETE CASCADE;

-- Add foreign key constraint from hierarchy to hierarchy_def (deferred to avoid circular dependency)
ALTER TABLE hierarchy
ALTER COLUMN hierarchy_def_owner_id SET NOT NULL,
ALTER COLUMN hierarchy_def_name SET NOT NULL,
ADD CONSTRAINT fk_hierarchy_def FOREIGN KEY (hierarchy_def_owner_id, hierarchy_def_name) REFERENCES hierarchy_def(owner_id, name);

-- ========== INDEXES FOR PERFORMANCE ==========

-- AspectDef indexes
CREATE INDEX idx_aspect_def_name ON aspect_def(name);

-- PropertyDef indexes
CREATE INDEX idx_property_def_aspect_def_id ON property_def(aspect_def_id);
CREATE INDEX idx_property_def_name ON property_def(name);

-- HierarchyDefOwner indexes
CREATE INDEX idx_hierarchy_def_owner_type ON hierarchy_def_owner(owner_type_code);
CREATE INDEX idx_hierarchy_def_owner_catalog_def_id ON hierarchy_def_owner(catalog_def_id);
CREATE INDEX idx_hierarchy_def_owner_catalog_id ON hierarchy_def_owner(catalog_id);

-- HierarchyDef indexes
CREATE INDEX idx_hierarchy_def_owner_id ON hierarchy_def(owner_id);
CREATE INDEX idx_hierarchy_def_name ON hierarchy_def(name);
CREATE INDEX idx_hierarchy_def_type ON hierarchy_def(hierarchy_type);

-- CatalogDef indexes
CREATE INDEX idx_catalog_def_name ON catalog_def(name);

-- Catalog indexes
CREATE INDEX idx_catalog_def_id ON catalog(catalog_def_id);
CREATE INDEX idx_catalog_species ON catalog(species);
CREATE INDEX idx_catalog_upstream ON catalog(upstream_catalog_id);

-- Hierarchy indexes
CREATE INDEX idx_hierarchy_catalog_id ON hierarchy(catalog_id);
CREATE INDEX idx_hierarchy_hierarchy_def_owner_id ON hierarchy(hierarchy_def_owner_id);
CREATE INDEX idx_hierarchy_hierarchy_def_name ON hierarchy(hierarchy_def_name);
CREATE INDEX idx_hierarchy_name ON hierarchy(name);

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
CREATE INDEX idx_property_value_type ON property_value(value_type);

-- Hierarchy content indexes
CREATE INDEX idx_hierarchy_entity_list_catalog_id ON hierarchy_entity_list(hierarchy_catalog_id);
CREATE INDEX idx_hierarchy_entity_list_name ON hierarchy_entity_list(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_list_entity_id ON hierarchy_entity_list(entity_id);
CREATE INDEX idx_hierarchy_entity_list_order ON hierarchy_entity_list(list_order);

CREATE INDEX idx_hierarchy_entity_set_catalog_id ON hierarchy_entity_set(hierarchy_catalog_id);
CREATE INDEX idx_hierarchy_entity_set_name ON hierarchy_entity_set(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_set_entity_id ON hierarchy_entity_set(entity_id);

CREATE INDEX idx_hierarchy_entity_directory_catalog_id ON hierarchy_entity_directory(hierarchy_catalog_id);
CREATE INDEX idx_hierarchy_entity_directory_name ON hierarchy_entity_directory(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_directory_entity_id ON hierarchy_entity_directory(entity_id);
CREATE INDEX idx_hierarchy_entity_directory_key ON hierarchy_entity_directory(entity_key);

CREATE INDEX idx_hierarchy_entity_tree_catalog_id ON hierarchy_entity_tree_node(hierarchy_catalog_id);
CREATE INDEX idx_hierarchy_entity_tree_name ON hierarchy_entity_tree_node(hierarchy_name);
CREATE INDEX idx_hierarchy_entity_tree_parent_id ON hierarchy_entity_tree_node(parent_node_id);
CREATE INDEX idx_hierarchy_entity_tree_entity_id ON hierarchy_entity_tree_node(entity_id);
CREATE INDEX idx_hierarchy_entity_tree_path ON hierarchy_entity_tree_node(node_path);

CREATE INDEX idx_hierarchy_aspect_map_catalog_id ON hierarchy_aspect_map(hierarchy_catalog_id);
CREATE INDEX idx_hierarchy_aspect_map_name ON hierarchy_aspect_map(hierarchy_name);
CREATE INDEX idx_hierarchy_aspect_map_entity_id ON hierarchy_aspect_map(entity_id);
CREATE INDEX idx_hierarchy_aspect_map_aspect_def_id ON hierarchy_aspect_map(aspect_def_id);

-- ========== COMMENTS FOR DOCUMENTATION ==========

COMMENT ON TABLE aspect_def IS 'Defines aspect structure and metadata - first-class entities';
COMMENT ON TABLE property_def IS 'Defines property structure within AspectDefs';
COMMENT ON TABLE hierarchy_def_owner_type IS 'Discriminator table for hierarchy definition ownership types';
COMMENT ON TABLE hierarchy_def_owner IS 'Supertype table that can reference either CatalogDef or Hierarchy as owners';
COMMENT ON TABLE hierarchy_def IS 'Defines hierarchy structure and metadata - can be owned by CatalogDef or Hierarchy';
COMMENT ON TABLE catalog_def IS 'Defines catalog structure and characteristics';
COMMENT ON TABLE entity IS 'Conceptual entities with only global ID';
COMMENT ON TABLE catalog IS 'Catalog instances extending Entity';
COMMENT ON TABLE hierarchy IS 'Hierarchy instances within catalogs';
COMMENT ON TABLE aspect IS 'Aspect instances attached to entities';
COMMENT ON TABLE property_value IS 'Generic property value storage - one row per property value';

COMMENT ON TABLE hierarchy_entity_list IS 'Entity List Hierarchy: Ordered list with possible duplicates';
COMMENT ON TABLE hierarchy_entity_set IS 'Entity Set Hierarchy: Unique entities (possibly ordered)';
COMMENT ON TABLE hierarchy_entity_directory IS 'Entity Directory Hierarchy: String-to-entity mapping';
COMMENT ON TABLE hierarchy_entity_tree_node IS 'Entity Tree Hierarchy: Tree structure with named nodes';
COMMENT ON TABLE hierarchy_aspect_map IS 'Aspect Map Hierarchy: Entity-to-aspect mapping for single aspect type';

COMMENT ON COLUMN property_value.value_text IS 'Storage for STR, TXT, BGI, BGF, URI, UID, CLB types';
COMMENT ON COLUMN property_value.value_integer IS 'Storage for INT type (stored as BIGINT for 64-bit range)';
COMMENT ON COLUMN property_value.value_float IS 'Storage for FLT type (stored as DOUBLE PRECISION)';
COMMENT ON COLUMN property_value.value_boolean IS 'Storage for BLN type';
COMMENT ON COLUMN property_value.value_datetime IS 'Storage for DAT type';
COMMENT ON COLUMN property_value.value_binary IS 'Storage for BLB type';