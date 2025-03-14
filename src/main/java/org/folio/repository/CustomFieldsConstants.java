package org.folio.repository;

public final class CustomFieldsConstants {

  public static final String CUSTOM_FIELDS_TABLE = "custom_fields";

  public static final String MAX_ORDER_COLUMN = "max_order";
  public static final String VALUES_COLUMN = "values";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String ID_COLUMN = "id";

  public static final String REF_ID_REGEX = "%s(_([2-9]|[1-9]\\d+))?";
  public static final String SELECT_REF_IDS = "SELECT "+ JSONB_COLUMN + " ->> 'refId' as "
    + VALUES_COLUMN + " FROM %s WHERE " + JSONB_COLUMN + " ->> 'refId' SIMILAR TO $1" ;
  public static final String SELECT_MAX_ORDER = "SELECT MAX((jsonb ->> 'order')::int) as " + MAX_ORDER_COLUMN + " FROM %s";

  private CustomFieldsConstants() {
  }
}
