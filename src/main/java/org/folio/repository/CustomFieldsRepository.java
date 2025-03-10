package org.folio.repository;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldCollection;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.SQLConnection;

public interface CustomFieldsRepository {

  /**
   * Saves the definition of the custom field.
   * Returns newly created custom field.
   *
   * @param entity   - current definition of the custom field {@link CustomField} object to save
   * @param tenantId - tenant id
   */
  Future<CustomField> save(CustomField entity, String tenantId);

  /**
   * Saves the definition of the custom field.
   * Returns newly created custom field.
   *
   * @param entity     - current definition of the custom field {@link CustomField} object to save
   * @param tenantId   - tenant id
   * @param connection - {@link SQLConnection} to use
   */
  Future<CustomField> save(CustomField entity, String tenantId, @Nullable AsyncResult<SQLConnection> connection);

  Future<CustomField> save(CustomField entity, String tenantId,
                           @Nullable Conn connection);

  /**
   * Fetches a custom field definition with given id
   * If custom field with given id doesn't exist then returns an empty {@link Optional}.
   *
   * @param id       - id of custom field to get
   * @param tenantId - tenant id
   */
  Future<Optional<CustomField>> findById(String id, String tenantId);

  /**
   * Fetches the maximum custom field reference id by given custom field name.
   *
   * @param customFieldName - name of custom field
   * @param tenantId        - tenant id
   */
  Future<Integer> maxRefId(String customFieldName, String tenantId);

  /**
   * Fetches the maximum custom field reference id by given custom field name.
   *
   * @param customFieldName - name of custom field
   * @param tenantId        - tenant id
   * @param connection      - {@link SQLConnection} to use
   */
  Future<Integer> maxRefId(String customFieldName, String tenantId, @Nullable AsyncResult<SQLConnection> connection);

  Future<Integer> maxRefId(String customFieldName, String tenantId,
                           @Nullable Conn connection);

  /**
   * Fetches the maximum value of "order" attribute in all custom fields,
   * or 0 if there are no fields with "order" attribute.
   *
   * @param tenantId - tenant id
   */
  Future<Integer> maxOrder(String tenantId);


  /**
   * Fetches custom fields collection that match specified CQL query.
   *
   * @param query    - CQL query
   * @param offset   - offset
   * @param limit    - limit
   * @param tenantId - tenant id
   */
  Future<CustomFieldCollection> findByQuery(String query, int offset, int limit, String tenantId);

  /**
   * Updates custom field definition.
   *
   * @param entity   - entity to update
   * @param tenantId - tenant id
   */
  Future<Boolean> update(CustomField entity, String tenantId);

  /**
   * Updates custom field definition.
   *
   * @param entity     - entity to update
   * @param tenantId   - tenant id
   * @param connection - {@link SQLConnection} to use
   */
  Future<Boolean> update(CustomField entity, String tenantId, @Nullable AsyncResult<SQLConnection> connection);

  Future<Boolean> update(CustomField entity, String tenantId, @Nullable Conn connection);

  /**
   * Deletes custom field with given id.
   *
   * @param id       - id of custom field to delete
   * @param tenantId - tenant id
   */
  Future<Boolean> delete(String id, String tenantId);

  /**
   * Deletes custom field with given id.
   *
   * @param id         - id of custom field to delete
   * @param tenantId   - tenant id
   * @param connection - {@link SQLConnection} to use
   */
  Future<Boolean> delete(String id, String tenantId, @Nullable AsyncResult<SQLConnection> connection);

  Future<Boolean> delete(String id, String tenantId, @Nonnull Conn connection);
}
