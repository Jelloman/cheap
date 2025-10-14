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

INSERT INTO test_aspect_mapping_no_key (string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col)
VALUES
	('row1', 100, 1.5, '2024-01-01', '2024-01-01 10:00:00', TRUE, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '\xDEADBEEF'),
	('row2', 200, 2.5, '2024-01-02', '2024-01-02 11:00:00', FALSE, 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', '\xCAFEBABE');

CREATE TABLE test_aspect_mapping_with_cat_id (
	catalog_id UUID NOT NULL,
	string_col TEXT,
	integer_col INTEGER,
	float_col DOUBLE PRECISION,
	date_col DATE,
	timestamp_col TIMESTAMP,
	boolean_col BOOLEAN,
	uuid_col UUID,
	blob_col BYTEA
);

INSERT INTO test_aspect_mapping_with_cat_id (catalog_id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col)
VALUES
	('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'cat_row1', 300, 3.5, '2024-02-01', '2024-02-01 12:00:00', TRUE, 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', '\xDEADBEEF'),
	('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'cat_row2', 400, 4.5, '2024-02-02', '2024-02-02 13:00:00', FALSE, 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', '\xCAFEBABE');

CREATE TABLE test_aspect_mapping_with_entity_id (
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

INSERT INTO test_aspect_mapping_with_entity_id (entity_id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col)
VALUES
	('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'entity_row1', 500, 5.5, '2024-03-01', '2024-03-01 14:00:00', TRUE, '10eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', '\xDEADBEEF'),
	('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'entity_row2', 600, 6.5, '2024-03-02', '2024-03-02 15:00:00', FALSE, '20eebc99-9c0b-4ef8-bb6d-6bb9bd380a66', '\xCAFEBABE');

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

INSERT INTO test_aspect_mapping_with_both_ids (catalog_id, entity_id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col)
VALUES
	('30eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '40eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'both_row1', 700, 7.5, '2024-04-01', '2024-04-01 16:00:00', TRUE, '50eebc99-9c0b-4ef8-bb6d-6bb9bd380a77', '\xDEADBEEF'),
	('30eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '40eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'both_row2', 800, 8.5, '2024-04-02', '2024-04-02 17:00:00', FALSE, '60eebc99-9c0b-4ef8-bb6d-6bb9bd380a88', '\xCAFEBABE');

