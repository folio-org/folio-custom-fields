package org.folio.validate;

import io.vertx.core.json.Json;
import org.folio.rest.jaxrs.model.Errors;

public class CustomFieldValidationException extends RuntimeException {

  private Errors errors;

  public CustomFieldValidationException(Errors errors) {
    this.errors = errors;
  }

  @Override
  public String getMessage() {
    return Json.encode(errors.getErrors());
  }

  public Errors getErrors() {
    return errors;
  }
}
