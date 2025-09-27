-- PostgreSQL Drop DDL for Cheap Data Model
-- Drops all tables, functions, and other database objects created by postgres-cheap.ddl and postgres-cheap-audit.ddl
-- This script should be executed to completely clean up the Cheap schema

-- ========== DROP TRIGGERS ==========

-- Drop audit triggers (from postgres-cheap-audit.ddl)
DROP TRIGGER IF EXISTS update_aspect_def_updated_at ON aspect_def;
DROP TRIGGER IF EXISTS update_property_def_updated_at ON property_def;
DROP TRIGGER IF EXISTS update_hierarchy_def_updated_at ON hierarchy_def;
DROP TRIGGER IF EXISTS update_catalog_def_updated_at ON catalog_def;
DROP TRIGGER IF EXISTS update_catalog_updated_at ON catalog;
DROP TRIGGER IF EXISTS update_hierarchy_updated_at ON hierarchy;
DROP TRIGGER IF EXISTS update_aspect_updated_at ON aspect;
DROP TRIGGER IF EXISTS update_property_value_updated_at ON property_value;

-- ========== DROP FUNCTIONS ==========

-- Drop audit function (from postgres-cheap-audit.ddl)
DROP FUNCTION IF EXISTS update_updated_at_column();

-- ========== DROP HIERARCHY CONTENT TABLES ==========

-- Drop hierarchy content tables (from postgres-cheap.ddl)
DROP TABLE IF EXISTS hierarchy_aspect_map CASCADE;
DROP TABLE IF EXISTS hierarchy_entity_tree_node CASCADE;
DROP TABLE IF EXISTS hierarchy_entity_directory CASCADE;
DROP TABLE IF EXISTS hierarchy_entity_set CASCADE;
DROP TABLE IF EXISTS hierarchy_entity_list CASCADE;

-- ========== DROP PROPERTY VALUE STORAGE ==========

-- Drop property value table (from postgres-cheap.ddl)
DROP TABLE IF EXISTS property_value CASCADE;

-- ========== DROP CORE ENTITY TABLES ==========

-- Drop core entity tables (from postgres-cheap.ddl)
DROP TABLE IF EXISTS aspect CASCADE;
DROP TABLE IF EXISTS hierarchy CASCADE;
DROP TABLE IF EXISTS catalog_aspect_def CASCADE;
DROP TABLE IF EXISTS catalog CASCADE;
DROP TABLE IF EXISTS entity CASCADE;

-- ========== DROP LINK TABLES ==========

-- Drop link tables (from postgres-cheap.ddl)
DROP TABLE IF EXISTS catalog_def_hierarchy_def CASCADE;
DROP TABLE IF EXISTS catalog_def_aspect_def CASCADE;

-- ========== DROP CORE DEFINITION TABLES ==========

-- Drop core definition tables (from postgres-cheap.ddl)
DROP TABLE IF EXISTS catalog_def CASCADE;
DROP TABLE IF EXISTS hierarchy_def CASCADE;
DROP TABLE IF EXISTS property_def CASCADE;
DROP TABLE IF EXISTS aspect_def CASCADE;

-- ========== DROP EXTENSIONS ==========

-- Note: We don't drop the uuid-ossp extension as it might be used by other schemas
-- DROP EXTENSION IF EXISTS "uuid-ossp";

-- ========== CLEANUP COMPLETE ==========

-- All Cheap data model objects have been dropped