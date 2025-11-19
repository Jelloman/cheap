-- PostgreSQL Truncate DDL for Cheap Data Model
-- Truncates all tables in the Cheap schema to clean up test data
-- This is much faster than dropping and recreating tables

-- Disable triggers to avoid issues with foreign keys during truncation
SET session_replication_role = replica;

-- Truncate all tables in reverse dependency order
TRUNCATE TABLE hierarchy_aspect_map CASCADE;
TRUNCATE TABLE hierarchy_entity_tree_node CASCADE;
TRUNCATE TABLE hierarchy_entity_directory CASCADE;
TRUNCATE TABLE hierarchy_entity_set CASCADE;
TRUNCATE TABLE hierarchy_entity_list CASCADE;
TRUNCATE TABLE property_value CASCADE;
TRUNCATE TABLE aspect CASCADE;
TRUNCATE TABLE hierarchy CASCADE;
TRUNCATE TABLE catalog_aspect_def CASCADE;
TRUNCATE TABLE catalog CASCADE;
--TRUNCATE TABLE entity CASCADE;
TRUNCATE TABLE property_def CASCADE;
TRUNCATE TABLE aspect_def CASCADE;

-- Re-enable triggers
SET session_replication_role = DEFAULT;
