-- SQLite Audit DDL for Cheap Data Model
-- Adds audit columns (created_at, updated_at) and triggers to track data changes
-- This file assumes sqlite-cheap.sql has been executed first

-- SQLite Note: Timestamps are stored as TEXT in ISO8601 format ("YYYY-MM-DD HH:MM:SS")

-- ========== ADD AUDIT COLUMNS TO DEFINITION TABLES ==========

-- Add audit columns to aspect_def
ALTER TABLE aspect_def
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE aspect_def
ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to property_def
ALTER TABLE property_def
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE property_def
ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO LINK TABLES ==========

-- Add audit columns to catalog_aspect_def
ALTER TABLE catalog_aspect_def
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO CORE ENTITY TABLES ==========

-- Add audit columns to entity
--ALTER TABLE entity
--ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to catalog
ALTER TABLE catalog
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE catalog
ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy
ALTER TABLE hierarchy
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE hierarchy
ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to aspect
ALTER TABLE aspect
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE aspect
ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO PROPERTY VALUE STORAGE ==========

-- Add audit columns to property_value
ALTER TABLE property_value
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE property_value
ADD COLUMN updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO HIERARCHY CONTENT TABLES ==========

-- Add audit columns to hierarchy_entity_list
ALTER TABLE hierarchy_entity_list
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_entity_set
ALTER TABLE hierarchy_entity_set
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_entity_directory
ALTER TABLE hierarchy_entity_directory
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_entity_tree_node
ALTER TABLE hierarchy_entity_tree_node
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_aspect_map
ALTER TABLE hierarchy_aspect_map
ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== CREATE AUDIT INDEXES ==========

-- Entity index on created_at
--CREATE INDEX idx_entity_created_at ON entity(created_at);

-- ========== TRIGGERS FOR AUTOMATIC UPDATES ==========

-- Triggers to update the updated_at timestamp
-- SQLite Note: Triggers must be defined per table and inline the update logic

-- Trigger for aspect_def
CREATE TRIGGER update_aspect_def_updated_at
AFTER UPDATE ON aspect_def
FOR EACH ROW
BEGIN
    UPDATE aspect_def SET updated_at = CURRENT_TIMESTAMP
    WHERE aspect_def_id = NEW.aspect_def_id;
END;

-- Trigger for property_def
CREATE TRIGGER update_property_def_updated_at
AFTER UPDATE ON property_def
FOR EACH ROW
BEGIN
    UPDATE property_def SET updated_at = CURRENT_TIMESTAMP
    WHERE aspect_def_id = NEW.aspect_def_id AND name = NEW.name;
END;

-- Trigger for catalog
CREATE TRIGGER update_catalog_updated_at
AFTER UPDATE ON catalog
FOR EACH ROW
BEGIN
    UPDATE catalog SET updated_at = CURRENT_TIMESTAMP
    WHERE catalog_id = NEW.catalog_id;
END;

-- Trigger for hierarchy
CREATE TRIGGER update_hierarchy_updated_at
AFTER UPDATE ON hierarchy
FOR EACH ROW
BEGIN
    UPDATE hierarchy SET updated_at = CURRENT_TIMESTAMP
    WHERE catalog_id = NEW.catalog_id AND name = NEW.name;
END;

-- Trigger for aspect
CREATE TRIGGER update_aspect_updated_at
AFTER UPDATE ON aspect
FOR EACH ROW
BEGIN
    UPDATE aspect SET updated_at = CURRENT_TIMESTAMP
    WHERE entity_id = NEW.entity_id AND aspect_def_id = NEW.aspect_def_id AND catalog_id = NEW.catalog_id;
END;

-- Trigger for property_value
CREATE TRIGGER update_property_value_updated_at
AFTER UPDATE ON property_value
FOR EACH ROW
BEGIN
    UPDATE property_value SET updated_at = CURRENT_TIMESTAMP
    WHERE entity_id = NEW.entity_id AND aspect_def_id = NEW.aspect_def_id
        AND catalog_id = NEW.catalog_id AND property_name = NEW.property_name
        AND value_index = NEW.value_index;
END;
