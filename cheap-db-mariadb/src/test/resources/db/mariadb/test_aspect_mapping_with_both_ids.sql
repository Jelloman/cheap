CREATE TABLE test_aspect_mapping_with_both_ids (
	catalog_id VARCHAR(36) NOT NULL,
	entity_id VARCHAR(36) NOT NULL,
	string_col VARCHAR(255),
	integer_col BIGINT,
	PRIMARY KEY (catalog_id, entity_id)
);
