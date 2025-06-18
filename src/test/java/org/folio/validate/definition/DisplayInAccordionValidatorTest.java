package org.folio.validate.definition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.DisplayInAccordion;
import org.folio.spring.TestConfiguration;
import org.folio.test.util.TestUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DisplayInAccordionValidatorTest {

  @Autowired private DisplayInAccordionValidator validator;
  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void shouldPassForValidField() {
    var customField = customField("user", DisplayInAccordion.EXTENDED_INFORMATION);

    validator.validateDefinition(customField);
    assertTrue(validator.isApplicable(customField));
  }

  @Test
  public void shouldFailOnUnknownField() {
    expectedEx.expect(IllegalArgumentException.class);
    expectedEx.expectMessage("'default' value is not allowed for displayInAccordion, " +
      "only the following values are allowed for entity type 'unknown': []");
    var customField = customField("unknown", DisplayInAccordion.DEFAULT);

    assertTrue(validator.isApplicable(customField));
    validator.validateDefinition(customField);
  }

  @Test
  public void shouldSkipCustomFieldWithoutRequiredField() throws Exception {
    final CustomField customField = TestUtil.readJsonFile(
      "fields/post/postCustomNameWithTooLongName.json", CustomField.class);
    assertFalse(validator.isApplicable(customField));
  }

  private static CustomField customField(String entityType, DisplayInAccordion value) {
    return new CustomField()
      .withEntityType(entityType)
      .withDisplayInAccordion(value);
  }
}
