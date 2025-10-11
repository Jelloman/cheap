
INSERT INTO test_table
(id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col)
VALUES(1, 'string1', 1, 1.5, '2025-01-01', '2025-01-11 18:18:18.018', true,
        '4186bfb6-b135-48af-9236-95cacdb20327'::uuid, decode('48F308FB637E67EC3B27B09400914BEA','hex'));

INSERT INTO test_table
(id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col)
VALUES(2, 'string2', 2, 2.5, '2025-02-02', '2025-02-02 02:02:02.002', false,
        '655a99b9-af7c-4f2f-afa8-c4801986b9d4'::uuid, decode('013D7D16D7AE67EC3B27B95B765C8CEB','hex'));


