package org.folio.validate.definition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.DisplayInAccordion;
import org.folio.spring.TestConfiguration;
import org.folio.test.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DisplayInAccordionValidatorTest {

  @Autowired private DisplayInAccordionValidator validator;

  @Test
  public void shouldPassForValidField() {
    var customField = customField("user", DisplayInAccordion.EXTENDED_INFORMATION);

    validator.validateDefinition(customField);
    assertTrue(validator.isApplicable(customField));
  }

  @Test
  public void shouldFailOnUnknownField() {
    var expectedMessage = "'default' value is not allowed for displayInAccordion, " +
      "only the following values are allowed for entity type 'unknown': []";
    var customField = customField("unknown", DisplayInAccordion.DEFAULT);

    assertTrue(validator.isApplicable(customField));
    var throwable = assertThrows(IllegalArgumentException.class,
      () -> validator.validateDefinition(customField));
    assertEquals(expectedMessage, throwable.getMessage());
  }

  @Test
  public void shouldSkipCustomFieldWithoutRequiredField() {
    var customField = new CustomField();
    assertFalse(validator.isApplicable(customField));
  }

  private static CustomField customField(String entityType, DisplayInAccordion value) {
    return new CustomField()
      .withEntityType(entityType)
      .withDisplayInAccordion(value);
  }
}
