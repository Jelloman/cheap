-- MariaDB Truncate DDL for Cheap Data Model
-- Truncates all tables in the Cheap schema to clean up test data
-- This is much faster than dropping and recreating tables

-- Disable foreign key checks to avoid issues during truncation
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate all tables in reverse dependency order
TRUNCATE TABLE hierarchy_aspect_map;
TRUNCATE TABLE hierarchy_entity_tree_node;
TRUNCATE TABLE hierarchy_entity_directory;
TRUNCATE TABLE hierarchy_entity_set;
TRUNCATE TABLE hierarchy_entity_list;
TRUNCATE TABLE property_value;
TRUNCATE TABLE aspect;
TRUNCATE TABLE hierarchy;
TRUNCATE TABLE catalog_aspect_def;
TRUNCATE TABLE catalog;
TRUNCATE TABLE entity;
TRUNCATE TABLE property_def;
TRUNCATE TABLE aspect_def;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
