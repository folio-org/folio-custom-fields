CREATE TABLE IF NOT EXISTS custom_fields(id UUID PRIMARY KEY, jsonb JSONB NOT NULL CHECK ((jsonb ->> 'refId'::text) IS NOT NULL));

DROP TRIGGER IF EXISTS set_id_injson_custom_fields ON custom_fields CASCADE;
DROP TRIGGER IF EXISTS set_id_in_jsonb ON custom_fields CASCADE;
CREATE TRIGGER set_id_in_jsonb BEFORE INSERT OR UPDATE ON custom_fields FOR EACH ROW EXECUTE PROCEDURE set_id_in_jsonb();

CREATE OR REPLACE FUNCTION set_custom_fields_md_json()
    RETURNS TRIGGER
AS $$
 DECLARE
    createdDate timestamp WITH TIME ZONE;
    createdBy text ;
    updatedDate timestamp WITH TIME ZONE;
    updatedBy text ;
    injectedMetadata text;
    createdByUsername text;
    updatedByUsername text;
 BEGIN
    createdBy = OLD.jsonb->'metadata'->>'createdByUserId';
    createdDate = OLD.jsonb->'metadata'->>'createdDate';
    createdByUsername = OLD.jsonb->'metadata'->>'createdByUsername';
    updatedBy = NEW.jsonb->'metadata'->>'updatedByUserId';
    updatedDate = NEW.jsonb->'metadata'->>'updatedDate';
    updatedByUsername = NEW.jsonb->'metadata'->>'updatedByUsername';
    injectedMetadata = jsonb_strip_nulls(
        json_build_object(
            'createdDate', to_char(createdDate,'YYYY-MM-DD"T"HH24:MI:SS.MSTZH:TZM'),
            'createdByUserId', createdBy,
            'createdByUsername', createdByUsername,
            'updatedDate', to_char(updatedDate,'YYYY-MM-DD"T"HH24:MI:SS.MSTZH:TZM'),
            'updatedByUserId', updatedBy,
            'updatedByUsername', updatedByUsername
        )::jsonb
    );
    NEW.jsonb = jsonb_set(NEW.jsonb, '{metadata}', injectedMetadata::jsonb, false);
    RETURN NEW;
 END;
$$
language 'plpgsql';

DROP TRIGGER IF EXISTS set_custom_fields_md_json_trigger ON custom_fields CASCADE;

CREATE TRIGGER set_custom_fields_md_json_trigger BEFORE UPDATE ON custom_fields   FOR EACH ROW EXECUTE PROCEDURE set_custom_fields_md_json();

CREATE OR REPLACE FUNCTION update_ref_id()
RETURNS TRIGGER AS $$
DECLARE
    newRefId text;
    oldRefId text;
    newCustomFieldName text;
    oldCustomFieldName text;
BEGIN
  newRefId = NEW.jsonb->'refId';
  oldRefId = OLD.jsonb->'refId';
  newCustomFieldName = NEW.jsonb->'name';
  oldCustomFieldName = OLD.jsonb->'name';

  if newRefId ISNULL                         then     newRefId := oldRefId;   end if;
  if newCustomFieldName = oldCustomFieldName then     newRefId := oldRefId;   end if;

  NEW.jsonb = jsonb_set(NEW.jsonb, '{refId}' ,  newRefId::jsonb , false);

  RETURN NEW;
END;
$$ language 'plpgsql';
DROP TRIGGER IF EXISTS update_ref_id_trigger ON custom_fields;
CREATE TRIGGER update_ref_id_trigger BEFORE UPDATE ON custom_fields FOR EACH ROW EXECUTE PROCEDURE update_ref_id();

