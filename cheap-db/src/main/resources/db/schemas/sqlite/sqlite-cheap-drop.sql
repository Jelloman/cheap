-- SQLite Drop DDL for Cheap Data Model
-- Drops all tables, triggers, and other database objects created by sqlite-cheap.sql and sqlite-cheap-audit.sql
-- This script should be executed to completely clean up the Cheap schema

-- ========== DROP TRIGGERS ==========

-- Drop audit triggers (from sqlite-cheap-audit.sql)
DROP TRIGGER IF EXISTS update_aspect_def_updated_at;
DROP TRIGGER IF EXISTS update_property_def_updated_at;
DROP TRIGGER IF EXISTS update_catalog_updated_at;
DROP TRIGGER IF EXISTS update_hierarchy_updated_at;
DROP TRIGGER IF EXISTS update_aspect_updated_at;
DROP TRIGGER IF EXISTS update_property_value_updated_at;

-- ========== DROP HIERARCHY CONTENT TABLES ==========

-- Drop hierarchy content tables (from sqlite-cheap.sql)
DROP TABLE IF EXISTS hierarchy_aspect_map;
DROP TABLE IF EXISTS hierarchy_entity_tree_node;
DROP TABLE IF EXISTS hierarchy_entity_directory;
DROP TABLE IF EXISTS hierarchy_entity_set;
DROP TABLE IF EXISTS hierarchy_entity_list;

-- ========== DROP PROPERTY VALUE STORAGE ==========

-- Drop property value table (from sqlite-cheap.sql)
DROP TABLE IF EXISTS property_value;

-- ========== DROP CORE ENTITY TABLES ==========

-- Drop core entity tables (from sqlite-cheap.sql)
DROP TABLE IF EXISTS aspect;
DROP TABLE IF EXISTS hierarchy;
DROP TABLE IF EXISTS catalog_aspect_def;
DROP TABLE IF EXISTS catalog;
DROP TABLE IF EXISTS entity;

-- ========== DROP DEFINITION TABLES ==========

-- Drop definition tables (from sqlite-cheap.sql)
DROP TABLE IF EXISTS property_def;
DROP TABLE IF EXISTS aspect_def;

-- ========== CLEANUP COMPLETE ==========

-- All Cheap data model objects have been dropped
