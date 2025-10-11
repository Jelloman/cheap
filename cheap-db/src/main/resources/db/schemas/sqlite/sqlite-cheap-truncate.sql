-- SQLite Truncate DDL for Cheap Data Model
-- Truncates all tables in the Cheap schema to clean up test data
-- This is much faster than dropping and recreating tables

-- SQLite Note: SQLite does not support TRUNCATE TABLE, use DELETE FROM instead
-- Temporarily disable foreign key constraints to allow deletion in any order
PRAGMA foreign_keys = OFF;

-- Delete all data from tables in reverse dependency order
DELETE FROM hierarchy_aspect_map;
DELETE FROM hierarchy_entity_tree_node;
DELETE FROM hierarchy_entity_directory;
DELETE FROM hierarchy_entity_set;
DELETE FROM hierarchy_entity_list;
DELETE FROM property_value;
DELETE FROM aspect;
DELETE FROM hierarchy;
DELETE FROM catalog_aspect_def;
DELETE FROM catalog;
DELETE FROM entity;
DELETE FROM property_def;
DELETE FROM aspect_def;

-- Re-enable foreign key constraints
PRAGMA foreign_keys = ON;

-- Optionally vacuum to reclaim space (comment out if not needed)
-- VACUUM;
