-- MariaDB Audit DDL for Cheap Data Model
-- Adds audit columns (created_at, updated_at) and triggers to track data changes
-- This file assumes mariadb-cheap.sql has been executed first

-- ========== ADD AUDIT COLUMNS TO DEFINITION TABLES ==========

-- Add audit columns to aspect_def
ALTER TABLE aspect_def
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- Add audit columns to property_def
ALTER TABLE property_def
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- ========== ADD AUDIT COLUMNS TO LINK TABLES ==========

-- Add audit columns to catalog_aspect_def
ALTER TABLE catalog_aspect_def
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- ========== ADD AUDIT COLUMNS TO CORE ENTITY TABLES ==========

-- Add audit columns to entity
-- ALTER TABLE entity
-- ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Add audit columns to catalog
ALTER TABLE catalog
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- Add audit columns to hierarchy
ALTER TABLE hierarchy
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- Add audit columns to aspect
ALTER TABLE aspect
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- ========== ADD AUDIT COLUMNS TO PROPERTY VALUE STORAGE ==========

-- Add audit columns to property_value
ALTER TABLE property_value
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- ========== ADD AUDIT COLUMNS TO HIERARCHY CONTENT TABLES ==========

-- Add audit columns to hierarchy_entity_list
ALTER TABLE hierarchy_entity_list
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Add audit columns to hierarchy_entity_set
ALTER TABLE hierarchy_entity_set
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Add audit columns to hierarchy_entity_directory
ALTER TABLE hierarchy_entity_directory
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Add audit columns to hierarchy_entity_tree_node
ALTER TABLE hierarchy_entity_tree_node
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Add audit columns to hierarchy_aspect_map
ALTER TABLE hierarchy_aspect_map
ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- ========== CREATE AUDIT INDEXES ==========

-- Entity index on created_at
-- CREATE INDEX idx_entity_created_at ON entity(created_at);
