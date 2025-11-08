-- MariaDB Foreign Key Constraints and Related Indexes for Cheap Data Model
-- This script is optional and can be run after mariadb-cheap.sql
-- Foreign keys provide referential integrity but can impact performance

-- ========== FOREIGN KEY CONSTRAINTS ==========

-- PropertyDef foreign keys
ALTER TABLE property_def
    ADD CONSTRAINT fk_property_def_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE;

-- Catalog-AspectDef link table foreign keys
ALTER TABLE catalog_aspect_def
    ADD CONSTRAINT fk_catalog_aspect_def_catalog FOREIGN KEY (catalog_id)
        REFERENCES catalog(catalog_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_catalog_aspect_def_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE;

-- Hierarchy foreign keys
ALTER TABLE hierarchy
    ADD CONSTRAINT fk_hierarchy_catalog FOREIGN KEY (catalog_id)
        REFERENCES catalog(catalog_id) ON DELETE CASCADE;

-- Aspect foreign keys
ALTER TABLE aspect
    ADD CONSTRAINT fk_aspect_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_aspect_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_aspect_catalog FOREIGN KEY (catalog_id)
        REFERENCES catalog(catalog_id) ON DELETE CASCADE;

-- Property value foreign keys
ALTER TABLE property_value
    ADD CONSTRAINT fk_property_value_aspect FOREIGN KEY (entity_id, aspect_def_id, catalog_id)
        REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE;

-- Hierarchy content table foreign keys
ALTER TABLE hierarchy_entity_list
    ADD CONSTRAINT fk_hierarchy_entity_list_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE;

ALTER TABLE hierarchy_entity_set
    ADD CONSTRAINT fk_hierarchy_entity_set_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE;

ALTER TABLE hierarchy_entity_directory
    ADD CONSTRAINT fk_hierarchy_entity_directory_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE;

ALTER TABLE hierarchy_entity_tree_node
    ADD CONSTRAINT fk_hierarchy_entity_tree_parent FOREIGN KEY (parent_node_id)
        REFERENCES hierarchy_entity_tree_node(node_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_hierarchy_entity_tree_entity FOREIGN KEY (entity_id)
        REFERENCES entity(entity_id) ON DELETE CASCADE;

ALTER TABLE hierarchy_aspect_map
    ADD CONSTRAINT fk_hierarchy_aspect_map_aspect_def FOREIGN KEY (aspect_def_id)
        REFERENCES aspect_def(aspect_def_id),
    ADD CONSTRAINT fk_hierarchy_aspect_map_aspect FOREIGN KEY (entity_id, aspect_def_id, catalog_id)
        REFERENCES aspect(entity_id, aspect_def_id, catalog_id) ON DELETE CASCADE;

-- ========== INDEXES ON FOREIGN KEY COLUMNS ==========

-- PropertyDef indexes on FK columns
CREATE INDEX idx_property_def_aspect_def_id ON property_def(aspect_def_id);

-- Catalog indexes on FK columns
CREATE INDEX idx_catalog_upstream ON catalog(upstream_catalog_id);

-- Hierarchy indexes on FK columns
CREATE INDEX idx_hierarchy_catalog_id ON hierarchy(catalog_id);

-- Aspect indexes on FK columns
CREATE INDEX idx_aspect_entity_id ON aspect(entity_id);
CREATE INDEX idx_aspect_def_id ON aspect(aspect_def_id);
CREATE INDEX idx_aspect_catalog_id ON aspect(catalog_id);

-- Property value indexes on FK columns
CREATE INDEX idx_property_value_entity_id ON property_value(entity_id);
CREATE INDEX idx_property_value_aspect_def_id ON property_value(aspect_def_id);
CREATE INDEX idx_property_value_catalog_id ON property_value(catalog_id);

-- Hierarchy content indexes on FK columns
CREATE INDEX idx_hierarchy_entity_list_entity_id ON hierarchy_entity_list(entity_id);
CREATE INDEX idx_hierarchy_entity_set_entity_id ON hierarchy_entity_set(entity_id);
CREATE INDEX idx_hierarchy_entity_directory_entity_id ON hierarchy_entity_directory(entity_id);
CREATE INDEX idx_hierarchy_entity_tree_parent_id ON hierarchy_entity_tree_node(parent_node_id);
CREATE INDEX idx_hierarchy_entity_tree_entity_id ON hierarchy_entity_tree_node(entity_id);
CREATE INDEX idx_hierarchy_aspect_map_entity_id ON hierarchy_aspect_map(entity_id);
CREATE INDEX idx_hierarchy_aspect_map_aspect_def_id ON hierarchy_aspect_map(aspect_def_id);
