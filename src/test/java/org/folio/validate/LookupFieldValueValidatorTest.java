package org.folio.validate;

import static org.folio.validate.ValidationTestUtil.parseCustomFieldJsonValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.folio.spring.TestConfiguration;
import org.folio.test.util.TestUtil;
import org.folio.validate.value.LookupFieldValueValidator;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class LookupFieldValueValidatorTest {

  private static final String VALID_UUID = "\"62d00c36-a94f-434d-9cd2-c7ea159303da\"";

  @Autowired
  private LookupFieldValueValidator validator;

  @Test
  public void shouldValidateWhenValueIsValidUuid() throws IOException, URISyntaxException {
    validator.validate(parseCustomFieldJsonValue(VALID_UUID), getLookupFieldDefinition());
  }

  @Test
  public void shouldValidateWhenValueIsNilUuid() throws IOException, URISyntaxException {
    String jsonValue = "\"00000000-0000-0000-0000-000000000000\"";
    validator.validate(parseCustomFieldJsonValue(jsonValue), getLookupFieldDefinition());
  }

  @Test
  public void shouldValidateWhenValueIsNonV4Uuid() throws IOException, URISyntaxException {
    String jsonValue = "\"017f22e2-79b0-7cc3-98c4-dc0c0c07398f\"";
    validator.validate(parseCustomFieldJsonValue(jsonValue), getLookupFieldDefinition());
  }

  @Test
  public void shouldThrowWhenValueIsNotAString() throws IOException, URISyntaxException {
    CustomField field = getLookupFieldDefinition();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> validator.validate(parseCustomFieldJsonValue("100"), field));
    assertEquals("Field with type LOOKUP must be a string", e.getMessage());
  }

  @Test
  public void shouldThrowWhenValueIsMalformedUuid() throws IOException, URISyntaxException {
    CustomField field = getLookupFieldDefinition();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> validator.validate(parseCustomFieldJsonValue("\"not-a-uuid\""), field));
    assertEquals("Field with type LOOKUP must contain a valid UUID: not-a-uuid", e.getMessage());
  }

  @Test
  public void shouldThrowWhenValueIsNonCanonicalUuid() throws IOException, URISyntaxException {
    CustomField field = getLookupFieldDefinition();
    assertThrows(IllegalArgumentException.class,
      () -> validator.validate(parseCustomFieldJsonValue("\"1-2-3-4-5\""), field));
  }

  @Test
  public void shouldThrowWhenNotRepeatableValueIsArray() throws IOException, URISyntaxException {
    CustomField field = getLookupFieldDefinition();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> validator.validate(parseCustomFieldJsonValue("[" + VALID_UUID + "]"), field));
    assertEquals("Field with type LOOKUP must be a string", e.getMessage());
  }

  @Test
  public void shouldValidateWhenRepeatableValueIsArrayOfUuids() throws IOException, URISyntaxException {
    String jsonValue = "[" + VALID_UUID + ", \"017f22e2-79b0-7cc3-98c4-dc0c0c07398f\"]";
    validator.validate(parseCustomFieldJsonValue(jsonValue), getLookupRepeatableFieldDefinition());
  }

  @Test
  public void shouldValidateWhenRepeatableValueIsEmptyArray() throws IOException, URISyntaxException {
    validator.validate(parseCustomFieldJsonValue("[]"), getLookupRepeatableFieldDefinition());
  }

  @Test
  public void shouldThrowWhenRepeatableValueIsScalar() throws IOException, URISyntaxException {
    CustomField field = getLookupRepeatableFieldDefinition();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> validator.validate(parseCustomFieldJsonValue(VALID_UUID), field));
    assertEquals("Field with type LOOKUP must be an array", e.getMessage());
  }

  @Test
  public void shouldThrowWhenRepeatableArrayHasMalformedUuid() throws IOException, URISyntaxException {
    CustomField field = getLookupRepeatableFieldDefinition();
    String jsonValue = "[" + VALID_UUID + ", \"not-a-uuid\"]";
    assertThrows(IllegalArgumentException.class,
      () -> validator.validate(parseCustomFieldJsonValue(jsonValue), field));
  }

  @Test
  public void testSupportedTypes() {
    assertEquals(List.of(Type.LOOKUP), validator.supportedTypes());
  }

  private CustomField getLookupFieldDefinition() throws IOException, URISyntaxException {
    return TestUtil.readJsonFile("fields/model/lookupField.json", CustomField.class);
  }

  private CustomField getLookupRepeatableFieldDefinition() throws IOException, URISyntaxException {
    return TestUtil.readJsonFile("fields/model/lookupRepeatableField.json", CustomField.class);
  }
}
