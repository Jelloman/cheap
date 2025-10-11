CREATE TABLE test_aspect_mapping_with_both_ids (
	catalog_id TEXT NOT NULL,
	entity_id TEXT NOT NULL,
	string_col TEXT,
	integer_col INTEGER,
	PRIMARY KEY (catalog_id, entity_id)
);
