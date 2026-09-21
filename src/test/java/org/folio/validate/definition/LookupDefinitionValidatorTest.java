package org.folio.validate.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.folio.rest.jaxrs.model.LookupField;
import org.folio.rest.jaxrs.model.SelectField;
import org.folio.spring.TestConfiguration;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class LookupDefinitionValidatorTest {

  @Autowired private LookupDefinitionValidator validator;

  @Test
  public void shouldBeApplicableForLookup() {
    assertTrue(validator.isApplicable(lookupField().withLookupField(new LookupField().withRefEntityType("organization"))));
  }

  @Test
  public void shouldNotBeApplicableForOtherTypes() {
    assertFalse(validator.isApplicable(lookupField().withType(Type.DATE_PICKER)));
  }

  @Test
  public void shouldValidateValidDefinition() {
    validator.validateDefinition(
      lookupField().withLookupField(new LookupField().withRefEntityType("organization")));
  }

  @Test
  public void shouldReturnErrorIfContainsNotAllowedFields() {
    CustomField customField = lookupField()
      .withLookupField(new LookupField().withRefEntityType("organization"))
      .withSelectField(new SelectField());
    IllegalArgumentException e =
      assertThrows(IllegalArgumentException.class, () -> validator.validateDefinition(customField));
    assertThat(e.getMessage(), containsString("Attribute selectField is not allowed"));
  }

  @Test
  public void shouldReturnErrorIfLookupFieldMissing() {
    CustomField customField = lookupField();
    IllegalArgumentException e =
      assertThrows(IllegalArgumentException.class, () -> validator.validateDefinition(customField));
    assertThat(e.getMessage(), containsString("The 'lookupField' property should be defined"));
  }

  @Test
  public void shouldReturnErrorIfRefEntityTypeBlank() {
    CustomField customField = lookupField().withLookupField(new LookupField().withRefEntityType("  "));
    IllegalArgumentException e =
      assertThrows(IllegalArgumentException.class, () -> validator.validateDefinition(customField));
    assertThat(e.getMessage(), containsString("The 'refEntityType' property should be defined"));
  }

  private CustomField lookupField() {
    return new CustomField()
      .withName("Lookup field")
      .withType(Type.LOOKUP)
      .withEntityType("po_line");
  }
}
