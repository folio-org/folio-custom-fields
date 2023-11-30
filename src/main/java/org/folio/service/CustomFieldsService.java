package org.folio.service;

import java.util.List;

import io.vertx.core.Future;

import org.folio.common.OkapiParams;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldCollection;
import org.folio.rest.jaxrs.model.CustomFieldOptionStatistic;
import org.folio.rest.jaxrs.model.CustomFieldStatistic;

public interface CustomFieldsService {

  /**
   * Saves the definition of the custom field.
   *
   * @param customField entity to save
   * @param params      OkapiParams with tenantId
   */
  Future<CustomField> save(CustomField customField, OkapiParams params);

  /**
   * Updates the definition of the custom field with specified id.
   *
   * @param id          id of field to update
   * @param customField new value
   * @param params      OkapiParams with tenantId
   */
  Future<Void> update(String id, CustomField customField, OkapiParams params);

  /**
   * Fetches custom field definition with given id.
   * If definition with given id doesn't exist then returns failed Future with NotFoundException as a cause.
   *
   * @param id       - id of custom field to fetch
   * @param tenantId - tenant id
   */
  Future<CustomField> findById(String id, String tenantId);

  /**
   * Fetches custom fields collection that match specified CQL query.
   *
   * @param query    - CQL query
   * @param offset   - offset
   * @param limit    - limit
   * @param tenantId - tenant id
   */
  Future<CustomFieldCollection> findByQuery(String query, int offset, int limit, String lang, String tenantId);

  /**
   * Deletes custom field with given id.
   *
   * @param id       - id of custom field to delete
   * @param tenantId - tenant id
   */
  Future<Void> delete(String id, String tenantId);

  /**
   * Replaces all existing custom fields with new collection of custom fields. If new collection has
   * fields with ids that already exist then those fields will be updated, fields with ids that
   * don't exist will be added, existing fields that are not present in new collection will be
   * deleted. Only custom fields with the specified entityType will be affected.
   *
   * @param newFields collection of new custom fields
   * @param entityType entityType of custom fields
   * @param params OkapiParams
   * @return updated collection of custom fields
   */
  Future<List<CustomField>> replaceAll(List<CustomField> newFields, String entityType, OkapiParams params);

  /**
   * Retrieves statistic of specific custom field definition usage.
   *
   * @param id       - id of custom field
   * @param tenantId - tenant id
   */
  Future<CustomFieldStatistic> retrieveStatistic(String id, String tenantId);

  /**
   * Retrieves statistic of specific custom field option usage. Can be processed only for selectable fields.
   *
   * @param id       - id of custom field
   * @param optId    - id of custom field option
   * @param tenantId - tenant id
   */
  Future<CustomFieldOptionStatistic> retrieveOptionStatistic(String id, String optId, String tenantId);
}
