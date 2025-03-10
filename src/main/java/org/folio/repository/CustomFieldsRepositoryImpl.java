package org.folio.repository;

import static org.folio.repository.CustomFieldsConstants.CUSTOM_FIELDS_TABLE;
import static org.folio.repository.CustomFieldsConstants.JSONB_COLUMN;
import static org.folio.repository.CustomFieldsConstants.MAX_ORDER_COLUMN;
import static org.folio.repository.CustomFieldsConstants.REF_ID_REGEX;
import static org.folio.repository.CustomFieldsConstants.SELECT_MAX_ORDER;
import static org.folio.repository.CustomFieldsConstants.SELECT_REF_IDS;
import static org.folio.repository.CustomFieldsConstants.VALUES_COLUMN;
import static org.folio.repository.CustomFieldsConstants.WHERE_ID_EQUALS_CLAUSE;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.persist.Conn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.CqlQuery;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.SQLConnection;
import org.folio.rest.persist.interfaces.Results;

@Log4j2
@Component
public class CustomFieldsRepositoryImpl implements CustomFieldsRepository {

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public Future<CustomField> save(CustomField entity, String tenantId) {
    return save(entity, tenantId, (AsyncResult<SQLConnection>)null);
  }


  @Override
  public Future<CustomField> save(CustomField entity, String tenantId,
                                  @Nullable AsyncResult<SQLConnection> connection) {
    Promise<String> promise = Promise.promise();
    setIdIfMissing(entity);
    log.debug("Saving a custom field with id: {}.", entity.getId());
    PostgresClient client = pgClient(tenantId);
    if (connection != null) {
      client.save(connection, CUSTOM_FIELDS_TABLE, entity.getId(), entity, promise);
    } else {
      client.save(CUSTOM_FIELDS_TABLE, entity.getId(), entity, promise);
    }

    return promise.future().map(id -> {
      entity.setId(id);
      return entity;
    }).recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<CustomField> save(CustomField entity, String tenantId, Conn connection) {
    setIdIfMissing(entity);
    log.debug("Saving a custom field with id: {}.", entity.getId());
    return connection.save(CUSTOM_FIELDS_TABLE, entity.getId(), entity).map(id -> {
      entity.setId(id);
      return entity;
    }).recover(excTranslator.translateOrPassBy());
  }


  @Override
  public Future<Optional<CustomField>> findById(String id, String tenantId) {
    Promise<CustomField> promise = Promise.promise();
    log.debug("Getting a custom field with id: {}.", id);
    pgClient(tenantId).getById(CUSTOM_FIELDS_TABLE, id, CustomField.class, promise);

    return promise.future().map(Optional::ofNullable)
      .recover(excTranslator.translateOrPassBy());
  }


  @Override
  public Future<Integer> maxRefId(String customFieldName, String tenantId) {
    return maxRefId(customFieldName, tenantId, (AsyncResult<SQLConnection>) null);
  }

  @Override
  public Future<Integer> maxRefId(String customFieldName, String tenantId,
                                  @Nullable AsyncResult<SQLConnection> connection) {
    Promise<RowSet<Row>> promise = Promise.promise();
    String query = String.format(SELECT_REF_IDS, getCFTableName(tenantId));
    String refIdRegex = String.format(REF_ID_REGEX, customFieldName);
    Tuple parameters = Tuple.of(refIdRegex);
    log.debug("Getting custom field ref ids by given name: {}.", customFieldName);
    PostgresClient client = pgClient(tenantId);
    if (connection != null) {
      client.select(connection, query, parameters, promise);
    } else {
      client.select(query, parameters, promise);
    }
    return promise.future().map(this::mapMaxRefId)
      .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<Integer> maxRefId(String customFieldName, String tenantId, Conn connection) {
    log.debug("Getting custom field ref ids by given name: {}.", customFieldName);
    String query = String.format(SELECT_REF_IDS, getCFTableName(tenantId));
    String refIdRegex = String.format(REF_ID_REGEX, customFieldName);
    Tuple parameters = Tuple.of(refIdRegex);
    return connection.execute(query, parameters).map(this::mapMaxRefId)
            .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<Integer> maxOrder(String tenantId) {
    Promise<Row> promise = Promise.promise();
    final String query = String.format(SELECT_MAX_ORDER, getCFTableName(tenantId));
    log.debug("Getting maximum order of custom fields.");
    pgClient(tenantId).selectSingle(query, promise);
    return promise.future().map(this::mapMaxOrder)
      .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<CustomFieldCollection> findByQuery(String query, int offset, int limit, String tenantId) {
    CqlQuery<CustomField> q = new CqlQuery<>(pgClient(tenantId), CUSTOM_FIELDS_TABLE, CustomField.class);
    log.debug("Getting custom fields by query: {}.", query);
    return q.get(query, offset, limit).map(this::toCustomFieldCollection)
      .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<Boolean> update(CustomField entity, String tenantId) {
    return update(entity, tenantId, (AsyncResult<SQLConnection>) null);
  }

  @Override
  public Future<Boolean> update(CustomField entity, String tenantId, @Nullable AsyncResult<SQLConnection> connection) {
    Promise<RowSet<Row>> promise = Promise.promise();
    log.debug("Updating a custom field with id: {}.", entity.getId());

    PostgresClient client = pgClient(tenantId);
    if (connection != null) {
      String whereClause = String.format(WHERE_ID_EQUALS_CLAUSE, entity.getId());
      client.update(connection, CUSTOM_FIELDS_TABLE, entity, JSONB_COLUMN, whereClause, false, promise);
    } else {
      client.update(CUSTOM_FIELDS_TABLE, entity, entity.getId(), promise);
    }
    return promise.future().map(rowSet -> rowSet.rowCount() == 1)
      .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<Boolean> update(CustomField entity, String tenantId, Conn connection) {
    log.debug("Updating a custom field with id: {}.", entity.getId());
    String whereClause = String.format(WHERE_ID_EQUALS_CLAUSE, entity.getId());
    return connection.update(CUSTOM_FIELDS_TABLE, entity, JSONB_COLUMN, whereClause, false)
            .map(rowSet -> rowSet.rowCount() == 1)
            .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<Boolean> delete(String id, String tenantId) {
    return delete(id, tenantId, (AsyncResult<SQLConnection>)null);
  }

  @Override
  public Future<Boolean> delete(String id, String tenantId, @Nullable AsyncResult<SQLConnection> connection) {
    Promise<RowSet<Row>> promise = Promise.promise();
    log.debug("Deleting custom field by given id: {}.", id);
    if (connection != null) {
      pgClient(tenantId).delete(connection, CUSTOM_FIELDS_TABLE, id, promise);
    } else {
      pgClient(tenantId).delete(CUSTOM_FIELDS_TABLE, id, promise);
    }
    return promise.future().map(rowSet -> rowSet.rowCount() == 1)
      .recover(excTranslator.translateOrPassBy());
  }

  @Override
  public Future<Boolean> delete(String id, String tenantId, Conn connection) {
    log.debug("Deleting custom field by given id: {}.", id);
    return connection.delete(CUSTOM_FIELDS_TABLE, id).map(rowSet -> rowSet.rowCount() == 1)
            .recover(excTranslator.translateOrPassBy());
  }

  private Integer mapMaxRefId(RowSet<Row> rowSet) {
    if (rowSet.rowCount() == 1 &&
      !RowSetUtils.mapFirstItem(rowSet, row -> row.getString(VALUES_COLUMN)).contains("_")) {
      return 1;
    }
    return RowSetUtils.streamOf(rowSet)
      .map(row -> row.getString(VALUES_COLUMN))
      .map(s -> StringUtils.substringAfter(s, "_"))
      .filter(StringUtils::isNotBlank)
      .mapToInt(Integer::parseInt)
      .max().orElse(0);
  }

  private Integer mapMaxOrder(Row result) {
    Integer maxOrder = result.getInteger(MAX_ORDER_COLUMN);
    return maxOrder != null ? maxOrder : 0;
  }

  private void setIdIfMissing(CustomField customField) {
    if (StringUtils.isBlank(customField.getId())) {
      customField.setId(UUID.randomUUID().toString());
    }
  }

  private CustomFieldCollection toCustomFieldCollection(Results<CustomField> results) {
    return new CustomFieldCollection()
      .withCustomFields(results.getResults())
      .withTotalRecords(results.getResultInfo().getTotalRecords());
  }

  private String getCFTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + CUSTOM_FIELDS_TABLE;
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
