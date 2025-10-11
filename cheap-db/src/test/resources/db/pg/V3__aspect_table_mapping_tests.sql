CREATE TABLE test_aspect_mapping_no_key (
	string_col TEXT,
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA
);

CREATE TABLE test_aspect_mapping_with_cat_id (
	catalog_id UUID NOT NULL,
	string_col TEXT,
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA,
);

CREATE TABLE test_aspect_mapping_with_entity_id (
	string_col TEXT,
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA
);

CREATE TABLE test_aspect_mapping_with_both_ids (
	catalog_id UUID NOT NULL,
	entity_id UUID NOT NULL,
	string_col TEXT,
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA,
	PRIMARY KEY (catalog_id, entity_id)
);

