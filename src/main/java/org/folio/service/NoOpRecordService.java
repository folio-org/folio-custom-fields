package org.folio.service;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.Future;

import org.folio.model.RecordUpdate;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldOptionStatistic;
import org.folio.rest.jaxrs.model.CustomFieldStatistic;
import org.folio.rest.persist.Conn;

public final class NoOpRecordService implements RecordService {

  @Override
  public Future<CustomFieldStatistic> retrieveStatistic(CustomField field, String tenantId) {
    return succeededFuture(
      new CustomFieldStatistic()
        .withFieldId(field.getId())
        .withEntityType(field.getEntityType())
        .withCount(0)
    );
  }

  @Override
  public Future<CustomFieldOptionStatistic> retrieveOptionStatistic(CustomField field, String optId, String tenantId) {
    return succeededFuture(
      new CustomFieldOptionStatistic()
        .withOptionId(optId)
        .withCustomFieldId(field.getId())
        .withEntityType(field.getEntityType())
        .withCount(0)
    );
  }

  @Override
  public Future<Void> deleteAllValues(CustomField field, String tenantId) {
    return succeededFuture();
  }

  @Override
  public Future<Void> deleteAllValues(Conn conn, CustomField field, String tenantId) {
    return succeededFuture();
  }

  @Override
  public Future<Void> deleteMissedOptionValues(RecordUpdate recordUpdate, String tenantId) {
    return succeededFuture();
  }

}
