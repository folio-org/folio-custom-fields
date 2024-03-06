package org.folio.service;

import static java.util.Collections.emptyList;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.folio.model.RecordUpdate;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldOptionStatistic;
import org.folio.rest.jaxrs.model.CustomFieldStatistic;
import org.folio.rest.jaxrs.model.SelectField;
import org.folio.rest.persist.PostgresClient;

public class RecordServiceImpl implements RecordService {

  private final Vertx vertx;
  private final Map<String, List<String>> entityTableMap;

  private RecordServiceImpl(Vertx vertx, Map<String, List<String>> entityTableMap) {
    this.vertx = vertx;
    this.entityTableMap = Objects.requireNonNull(entityTableMap);
  }

  /**
   * Factory method for creating a new instance of RecordService that works with multiple tables.
   * This method is used when there are multiple tables for a entityType that the service needs to
   * interact with.
   *
   * @param vertx          the Vertx instance.
   * @param entityTableMap a map where the key is the entity type and the value is a list of table
   *                       names associated with that entity type.
   * @return a new instance of RecordServiceImpl configured to work with the provided tables.
   */
  public static RecordService createForMultipleTables(
    Vertx vertx, Map<String, List<String>> entityTableMap) {
    return new RecordServiceImpl(vertx, entityTableMap);
  }

  /**
   * Factory method for creating a new instance of RecordService that works with single tables. This
   * method is used when there is a single table for each entityType that the service needs to
   * interact with.
   *
   * @param vertx          the Vertx instance.
   * @param entityTableMap a map where the key is the entity type and the value is the table name
   *                       associated with that entity type.
   * @return a new instance of RecordServiceImpl configured to work with the provided table.
   */
  public static RecordService createForSingleTable(
    Vertx vertx, Map<String, String> entityTableMap) {
    return new RecordServiceImpl(
      vertx,
      entityTableMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()))));
  }

  @Override
  public Future<CustomFieldStatistic> retrieveStatistic(CustomField field, String tenantId) {
    List<String> tableNames = getTableNames(field.getEntityType());
    List<Future<CustomFieldStatistic>> futures =
      tableNames.stream()
        .map(
          tableName -> {
            Promise<RowSet<Row>> replyHandler = Promise.promise();
            PostgresClient.getInstance(vertx, tenantId)
              .select(
                "SELECT COUNT(*) FROM " + tableName + " WHERE jsonb->'customFields' ? $1",
                Tuple.of(field.getRefId()),
                replyHandler);
            return replyHandler
              .future()
              .map(
                rs ->
                  createCustomFieldStatistic(
                    field, rs.iterator().next().getInteger(0)));
          })
        .toList();
    return Future.all(futures)
      .map(
        cf ->
          cf.list().stream()
            .map(o -> (CustomFieldStatistic) o)
            .reduce((s1, s2) -> s1.withCount(s1.getCount() + s2.getCount()))
            .orElse(createCustomFieldStatistic(field, 0)));
  }

  @Override
  public Future<CustomFieldOptionStatistic> retrieveOptionStatistic(
    CustomField field, String optId, String tenantId) {
    List<String> tableNames = getTableNames(field.getEntityType());

    String objectValue = isMultiSelect(field) ? "jsonb_build_array($2)" : "$2";
    List<Future<CustomFieldOptionStatistic>> futures =
      tableNames.stream()
        .map(
          tableName -> {
            Promise<RowSet<Row>> replyHandler = Promise.promise();
            PostgresClient.getInstance(vertx, tenantId)
              .select(
                "SELECT COUNT(*) FROM "
                  + tableName
                  + " WHERE jsonb->'customFields' @> jsonb_build_object($1, " + objectValue + ")",
                Tuple.of(field.getRefId(), optId),
                replyHandler);
            return replyHandler
              .future()
              .map(
                rs ->
                  createCustomFieldOptionStatistic(
                    field, optId, rs.iterator().next().getInteger(0)));
          })
        .toList();
    return Future.all(futures)
      .map(
        cf ->
          cf.list().stream()
            .map(o -> (CustomFieldOptionStatistic) o)
            .reduce((s1, s2) -> s1.withCount(s1.getCount() + s2.getCount()))
            .orElse(createCustomFieldOptionStatistic(field, optId, 0)));
  }

  @Override
  public Future<Void> deleteAllValues(CustomField field, String tenantId) {
    List<String> tableNames = getTableNames(field.getEntityType());
    List<Future<Void>> futures =
      tableNames.stream()
        .map(
          tableName -> {
            Promise<RowSet<Row>> replyHandler = Promise.promise();
            PostgresClient.getInstance(vertx, tenantId)
              .execute(
                "UPDATE "
                  + tableName
                  + " "
                  + "SET jsonb = jsonb_set(jsonb, '{customFields}', (jsonb->'customFields') - $1)"
                  + "WHERE jsonb->'customFields' ? $1",
                Tuple.of(field.getRefId()),
                replyHandler);
            return replyHandler.future().<Void>mapEmpty();
          })
        .toList();
    return Future.join(futures).mapEmpty();
  }

  @Override
  public Future<Void> deleteMissedOptionValues(RecordUpdate recordUpdate, String tenantId) {
    CustomField cf = recordUpdate.getCustomField();
    List<String> tableNames = getTableNames(cf.getEntityType());
    String opts = String.join(",", recordUpdate.getOptionIdsToDelete());

    String objectValue =
      isMultiSelect(recordUpdate.getCustomField())
        ? "jsonb_build_array(value)"
        : "value";

    List<Future<Void>> futures =
      tableNames.stream()
        .map(
          tableName -> {
            Promise<RowSet<Row>> replyHandler = Promise.promise();
            PostgresClient.getInstance(vertx, tenantId)
              .execute(
                "UPDATE "
                  + tableName
                  + " "
                  + "SET jsonb = jsonb_strip_nulls("
                  + "  jsonb_set("
                  + "    jsonb,"
                  + "    string_to_array('customFields,' || $1, ','),"
                  + "    ("
                  + "      SELECT"
                  + "        CASE"
                  + "          WHEN jsonb_typeof(jsonb->'customFields'->$1) = 'array' AND"
                  + "               jsonb_array_length((jsonb->'customFields'->$1) - string_to_array($2, ',')) > 0"
                  + "          THEN (jsonb->'customFields'->$1) - string_to_array($2, ',')"
                  + "          ELSE 'null'::jsonb"
                  + "        END AS value"
                  + "    )"
                  + "  )"
                  + ")"
                  + "WHERE jsonb->'customFields' @> ANY(ARRAY("
                  + "  SELECT jsonb_build_object($1, " + objectValue + ")"
                  + "  FROM unnest(string_to_array($2, ',')) AS value))",
                Tuple.of(cf.getRefId(), opts),
                replyHandler);
            return replyHandler.future().<Void>mapEmpty();
          })
        .toList();
    return Future.join(futures).mapEmpty();
  }

  private CustomFieldStatistic createCustomFieldStatistic(CustomField field, int count) {
    return new CustomFieldStatistic()
      .withFieldId(field.getId())
      .withEntityType(field.getEntityType())
      .withCount(count);
  }

  private CustomFieldOptionStatistic createCustomFieldOptionStatistic(
    CustomField field, String optId, int count) {
    return new CustomFieldOptionStatistic()
      .withCustomFieldId(field.getId())
      .withOptionId(optId)
      .withEntityType(field.getEntityType())
      .withCount(count);
  }

  private List<String> getTableNames(String entityType) {
    return entityType != null ? entityTableMap.getOrDefault(entityType, emptyList()) : emptyList();
  }

  private boolean isMultiSelect(CustomField field) {
    return Optional.ofNullable(field)
      .map(CustomField::getSelectField)
      .map(SelectField::getMultiSelect)
      .orElse(false);
  }
}
