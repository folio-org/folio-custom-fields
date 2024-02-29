package org.folio.validate;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.junit.Test;

public class CustomFieldValidationExceptionTest {

  @Test
  public void testGetMessage() {
    Error error = new Error().withMessage("Test error message");
    Errors errors = new Errors().withErrors(singletonList(error));
    CustomFieldValidationException exception = new CustomFieldValidationException(errors);

    String expectedMessage = "[{\"message\":\"Test error message\",\"parameters\":[]}]";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  public void testGetErrors() {
    Error error = new Error().withMessage("Test error message");
    Errors errors = new Errors().withErrors(singletonList(error));
    CustomFieldValidationException exception = new CustomFieldValidationException(errors);

    assertEquals(errors, exception.getErrors());
  }
}
