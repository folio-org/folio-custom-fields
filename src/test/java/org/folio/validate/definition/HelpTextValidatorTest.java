package org.folio.validate.definition;

import java.io.IOException;
import java.net.URISyntaxException;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.spring.TestConfiguration;
import org.folio.test.util.TestUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class HelpTextValidatorTest {

  @Autowired
  private HelpTextValidator validator;
  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void shouldReturnErrorIfIncorrectHelpTextLength() throws IOException, URISyntaxException {
    expectedEx.expect(IllegalArgumentException.class);
    expectedEx.expectMessage("The 'helpText' length cannot be more than 100");
    final CustomField customField = TestUtil.readJsonFile(
      "fields/post/postCustomFieldHelpTextInvalid.json", CustomField.class);
    validator.validateDefinition(customField);
  }

  @Test
  public void shouldPassValidationForHelpTextWithNull() throws IOException, URISyntaxException {
    Assert.assertFalse(expectedEx.isAnyExceptionExpected());
    final CustomField customField = TestUtil.readJsonFile(
      "fields/post/postCustomFieldHelpTextNull.json", CustomField.class);
    validator.validateDefinition(customField);
  }
}
