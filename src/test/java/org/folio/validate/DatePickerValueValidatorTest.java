package org.folio.validate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.folio.spring.TestConfiguration;
import org.folio.validate.value.DatePickerValueValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DatePickerValueValidatorTest {

  private static final CustomField customField =
      new CustomField().withType(Type.DATE_PICKER).withEntityType("user").withName("Date");
  @Autowired private DatePickerValueValidator validator;

  @Test
  public void shouldThrowIfInvalidType() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> validator.validate(true, customField));
    assertEquals("Field with type DATE_PICKER must be a string", e.getMessage());
  }

  @Test
  public void shouldThrowIfInvalidDateFormat() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> validator.validate("20231231", customField));
    assertEquals("Invalid date format: 20231231", e.getMessage());
  }

  @Test
  public void testValidDate() {
    validator.validate("2023-12-31", customField);
  }

  @Test
  public void testSupportedTypes() {
    assertEquals(List.of(Type.DATE_PICKER), validator.supportedTypes());
  }
}
