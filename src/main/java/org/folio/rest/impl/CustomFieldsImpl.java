package org.folio.rest.impl;

import static org.folio.rest.ResponseHelper.respond;
import static org.folio.rest.jaxrs.resource.CustomFields.PostCustomFieldsResponse.headersFor201;
import static org.folio.rest.jaxrs.resource.CustomFields.PostCustomFieldsResponse.respond201WithApplicationJson;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.folio.common.OkapiParams;
import org.folio.common.pf.PartialFunction;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldCollection;
import org.folio.rest.jaxrs.model.CustomFieldOptionStatistic;
import org.folio.rest.jaxrs.model.CustomFieldStatistic;
import org.folio.rest.jaxrs.model.PutCustomFieldCollection;
import org.folio.rest.jaxrs.resource.CustomFields;
import org.folio.service.CustomFieldsService;
import org.folio.spring.SpringContextUtil;
import org.folio.validate.definition.DefinitionValidator;

public class CustomFieldsImpl implements CustomFields {

  @Autowired
  private CustomFieldsService customFieldsService;

  @Autowired @Qualifier("customFieldsExcHandler")
  private PartialFunction<Throwable, Response> excHandler;

  @Autowired
  private DefinitionValidator definitionValidator;

  public CustomFieldsImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void postCustomFields(String lang, String xOkapiModuleId, CustomField entity, Map<String, String> okapiHeaders,
                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    definitionValidator.validate(entity);
    Future<CustomField> saved = customFieldsService.save(entity, new OkapiParams(okapiHeaders));
    respond(saved, customField -> respond201WithApplicationJson(customField, headersFor201()), asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putCustomFields(String xOkapiModuleId, PutCustomFieldCollection request, Map<String, String> okapiHeaders,
                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    List<CustomField> customFields = request.getCustomFields();
    customFields
      .forEach(definitionValidator::validate);
    customFields.forEach(field -> field.setMetadata(request.getMetadata()));
    Future<CustomFieldCollection> updatedFields = customFieldsService.replaceAll(customFields, request.getEntityType(), new OkapiParams(okapiHeaders))
      .map(fields -> new CustomFieldCollection()
        .withCustomFields(fields)
        .withTotalRecords(fields.size()));
    respond(updatedFields, fieldCollection -> PutCustomFieldsResponse.respond204(), asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getCustomFields(String query, int offset, int limit,
                              String lang, String xOkapiModuleId, Map<String, String> okapiHeaders,
                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    Future<CustomFieldCollection> found =
      customFieldsService.findByQuery(query, offset, limit, lang, tenantId(okapiHeaders));
    respond(found, GetCustomFieldsResponse::respond200WithApplicationJson, asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getCustomFieldsById(String id, String lang, Map<String, String> okapiHeaders,
                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    Future<CustomField> field = customFieldsService.findById(id, tenantId(okapiHeaders));
    respond(field, GetCustomFieldsByIdResponse::respond200WithApplicationJson, asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void deleteCustomFieldsById(String id, String lang, Map<String, String> okapiHeaders,
                                     Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    Future<Void> deleted = customFieldsService.delete(id, tenantId(okapiHeaders));
    respond(deleted, v -> DeleteCustomFieldsByIdResponse.respond204(), asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void putCustomFieldsById(String id, String lang, CustomField entity, Map<String, String> okapiHeaders,
                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    definitionValidator.validate(entity);

    Future<Void> updated = customFieldsService.update(id, entity, new OkapiParams(okapiHeaders));
    respond(updated, v -> PutCustomFieldsByIdResponse.respond204(), asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getCustomFieldsStatsById(String id, String lang, String xOkapiModuleId, Map<String, String> okapiHeaders,
                                       Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    Future<CustomFieldStatistic> stats = customFieldsService.retrieveStatistic(id, tenantId(okapiHeaders));

    respond(stats, GetCustomFieldsStatsByIdResponse::respond200WithApplicationJson, asyncResultHandler, excHandler);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getCustomFieldsOptionsStatsByIdAndOptId(String id, String optId,
                                                      String xOkapiModuleId, Map<String, String> okapiHeaders,
                                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                                      Context vertxContext) {
    Future<CustomFieldOptionStatistic> optionStatResult = customFieldsService
      .retrieveOptionStatistic(id, optId, tenantId(okapiHeaders));

    respond(optionStatResult,
      GetCustomFieldsOptionsStatsByIdAndOptIdResponse::respond200WithApplicationJson,
      asyncResultHandler, excHandler);
  }
}
