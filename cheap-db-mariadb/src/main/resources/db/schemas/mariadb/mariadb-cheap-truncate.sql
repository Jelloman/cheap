-- MariaDB Truncate DDL for Cheap Data Model
-- Truncates all tables in the Cheap schema to clean up test data
-- This is much faster than dropping and recreating tables

-- Disable foreign key checks to avoid issues during truncation
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate all tables in reverse dependency order
TRUNCATE TABLE hierarchy_aspect_map WAIT 50;
TRUNCATE TABLE hierarchy_entity_tree_node WAIT 50;
TRUNCATE TABLE hierarchy_entity_directory WAIT 50;
TRUNCATE TABLE hierarchy_entity_set WAIT 50;
TRUNCATE TABLE hierarchy_entity_list WAIT 50;
TRUNCATE TABLE property_value WAIT 50;
TRUNCATE TABLE aspect WAIT 50;
TRUNCATE TABLE hierarchy WAIT 50;
TRUNCATE TABLE catalog_aspect_def WAIT 50;
TRUNCATE TABLE catalog WAIT 50;
TRUNCATE TABLE entity WAIT 50;
TRUNCATE TABLE property_def WAIT 50;
TRUNCATE TABLE aspect_def WAIT 50;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
