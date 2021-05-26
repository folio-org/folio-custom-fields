package org.folio.rest.exceptions;

import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;

import static org.folio.common.pf.PartialFunctions.pf;
import static org.folio.rest.exc.ExceptionPredicates.instanceOf;

import javax.ws.rs.core.Response;


import io.vertx.core.json.Json;

import org.folio.common.pf.PartialFunction;
import org.folio.rest.ResponseHelper;
import org.folio.rest.jaxrs.model.Error;
import org.folio.service.exc.InvalidFieldValueException;
import org.folio.validate.ValidationUtil;

public class CustomFieldExceptionHandlers {

  private CustomFieldExceptionHandlers() {
  }

  public static PartialFunction<Throwable, Response> invalidOrderHandler() {
    return pf(instanceOf(IllegalArgumentException.class)
        .and(throwable -> {
          IllegalArgumentException exc = (IllegalArgumentException) throwable;
          return "Order number should be unique.".equals(exc.getMessage());
        })
      , CustomFieldExceptionHandlers::invalidOrderToUnprocessable);
  }

  public static PartialFunction<Throwable, Response> invalidValueHandler() {
    return pf(instanceOf(InvalidFieldValueException.class), CustomFieldExceptionHandlers::invalidValueToUnprocessable);
  }

  private static Response invalidOrderToUnprocessable(Throwable t) {
    IllegalArgumentException exc = (IllegalArgumentException) t;
    final Error errorMessage = ValidationUtil.createError(
      null, null, exc.getMessage());
    return ResponseHelper.statusWithJson(SC_UNPROCESSABLE_ENTITY, errorMessage);
  }

  private static Response invalidValueToUnprocessable(Throwable t) {
    InvalidFieldValueException exc = (InvalidFieldValueException) t;

    Error error = ValidationUtil.createError(Json.encode(exc.getValue()), exc.getField(), exc.getMessage());

    return ResponseHelper.statusWithJson(SC_UNPROCESSABLE_ENTITY, error);
  }
}
