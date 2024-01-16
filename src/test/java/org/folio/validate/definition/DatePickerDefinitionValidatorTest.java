package org.folio.validate.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.folio.rest.jaxrs.model.SelectField;
import org.folio.spring.TestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DatePickerDefinitionValidatorTest {

  private static final CustomField customField =
      new CustomField()
          .withName("Date field")
          .withType(Type.DATE_PICKER)
          .withEntityType("user")
          .withSelectField(new SelectField());
  @Autowired private DatePickerDefinitionValidator validator;

  @Test
  public void shouldBeApplicableForDatePicker() {
    assertTrue(validator.isApplicable(customField));
  }

  @Test
  public void shouldReturnErrorIfContainsNotAllowedFields() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> validator.validateDefinition(customField));
    assertThat(e.getMessage(), containsString("Attribute selectField is not allowed"));
  }
}
