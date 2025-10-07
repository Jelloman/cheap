
CREATE TABLE test_table (
	id SERIAL PRIMARY KEY,
	string_col VARCHAR(100),
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA
);

CREATE TABLE test_aspect_mapping (
	entity_id UUID NOT NULL PRIMARY KEY,
	string_col TEXT,
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA
);

