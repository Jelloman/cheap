-- PostgreSQL Audit DDL for Cheap Data Model
-- Adds audit columns (created_at, updated_at) and triggers to track data changes
-- This file assumes postgres-cheap.ddl has been executed first

-- ========== ADD AUDIT COLUMNS TO DEFINITION TABLES ==========

-- Add audit columns to aspect_def
ALTER TABLE aspect_def
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to property_def
ALTER TABLE property_def
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_def_owner
ALTER TABLE hierarchy_def_owner
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_def
ALTER TABLE hierarchy_def
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to catalog_def
ALTER TABLE catalog_def
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO LINK TABLES ==========

-- Add audit columns to catalog_def_aspect_def
ALTER TABLE catalog_def_aspect_def
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO CORE ENTITY TABLES ==========

-- Add audit columns to entity
ALTER TABLE entity
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to catalog
ALTER TABLE catalog
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to catalog_aspect_def
ALTER TABLE catalog_aspect_def
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy
ALTER TABLE hierarchy
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to aspect
ALTER TABLE aspect
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO PROPERTY VALUE STORAGE ==========

-- Add audit columns to property_value
ALTER TABLE property_value
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== ADD AUDIT COLUMNS TO HIERARCHY CONTENT TABLES ==========

-- Add audit columns to hierarchy_entity_list
ALTER TABLE hierarchy_entity_list
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_entity_set
ALTER TABLE hierarchy_entity_set
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_entity_directory
ALTER TABLE hierarchy_entity_directory
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_entity_tree_node
ALTER TABLE hierarchy_entity_tree_node
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add audit columns to hierarchy_aspect_map
ALTER TABLE hierarchy_aspect_map
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== CREATE AUDIT INDEXES ==========

-- Entity index on created_at (moved from main DDL)
CREATE INDEX idx_entity_created_at ON entity(created_at);

-- ========== TRIGGERS FOR AUTOMATIC UPDATES ==========

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply the trigger to all tables with updated_at columns
CREATE TRIGGER update_aspect_def_updated_at BEFORE UPDATE ON aspect_def
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_property_def_updated_at BEFORE UPDATE ON property_def
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hierarchy_def_owner_updated_at BEFORE UPDATE ON hierarchy_def_owner
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hierarchy_def_updated_at BEFORE UPDATE ON hierarchy_def
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_catalog_def_updated_at BEFORE UPDATE ON catalog_def
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_catalog_updated_at BEFORE UPDATE ON catalog
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hierarchy_updated_at BEFORE UPDATE ON hierarchy
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_aspect_updated_at BEFORE UPDATE ON aspect
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_property_value_updated_at BEFORE UPDATE ON property_value
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ========== AUDIT COMMENTS ==========

COMMENT ON COLUMN aspect_def.created_at IS 'Timestamp when the aspect definition was created';
COMMENT ON COLUMN aspect_def.updated_at IS 'Timestamp when the aspect definition was last updated';

COMMENT ON COLUMN property_def.created_at IS 'Timestamp when the property definition was created';
COMMENT ON COLUMN property_def.updated_at IS 'Timestamp when the property definition was last updated';

COMMENT ON COLUMN hierarchy_def_owner.created_at IS 'Timestamp when the hierarchy definition owner was created';
COMMENT ON COLUMN hierarchy_def_owner.updated_at IS 'Timestamp when the hierarchy definition owner was last updated';

COMMENT ON COLUMN hierarchy_def.created_at IS 'Timestamp when the hierarchy definition was created';
COMMENT ON COLUMN hierarchy_def.updated_at IS 'Timestamp when the hierarchy definition was last updated';

COMMENT ON COLUMN catalog_def.created_at IS 'Timestamp when the catalog definition was created';
COMMENT ON COLUMN catalog_def.updated_at IS 'Timestamp when the catalog definition was last updated';

COMMENT ON COLUMN entity.created_at IS 'Timestamp when the entity was created';

COMMENT ON COLUMN catalog.created_at IS 'Timestamp when the catalog was created';
COMMENT ON COLUMN catalog.updated_at IS 'Timestamp when the catalog was last updated';

COMMENT ON COLUMN hierarchy.created_at IS 'Timestamp when the hierarchy was created';
COMMENT ON COLUMN hierarchy.updated_at IS 'Timestamp when the hierarchy was last updated';

COMMENT ON COLUMN aspect.created_at IS 'Timestamp when the aspect was created';
COMMENT ON COLUMN aspect.updated_at IS 'Timestamp when the aspect was last updated';

COMMENT ON COLUMN property_value.created_at IS 'Timestamp when the property value was created';
COMMENT ON COLUMN property_value.updated_at IS 'Timestamp when the property value was last updated';

COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function to automatically update updated_at timestamp on row modifications';