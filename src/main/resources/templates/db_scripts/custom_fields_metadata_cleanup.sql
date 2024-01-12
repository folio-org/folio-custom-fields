-- Removes any entries inside metadata that have a value of "undefined" in custom_fields table.
-- The set_custom_fields_md_json_trigger trigger needs to be disabled for this operation.

ALTER TABLE ${myuniversity}_${mymodule}.custom_fields DISABLE TRIGGER set_custom_fields_md_json_trigger;

UPDATE ${myuniversity}_${mymodule}.custom_fields
SET jsonb = jsonb_set(
    jsonb,
    '{metadata}',
    (
        SELECT jsonb_object_agg(key, value)
        FROM jsonb_each(jsonb->'metadata')
        WHERE value IS DISTINCT FROM '"undefined"'
    )::jsonb
)
WHERE EXISTS (
    SELECT 1
    FROM jsonb_each(jsonb->'metadata')
    WHERE value = '"undefined"'
);

ALTER TABLE ${myuniversity}_${mymodule}.custom_fields ENABLE TRIGGER set_custom_fields_md_json_trigger;
