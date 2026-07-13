package org.folio.validate.value;

import static org.apache.commons.lang3.Validate.isInstanceOf;
import static org.apache.commons.lang3.Validate.isTrue;

import static org.folio.validate.value.CustomFieldValueValidatorConstants.EXPECT_ARRAY_MESSAGE;
import static org.folio.validate.value.CustomFieldValueValidatorConstants.EXPECT_STRING_MESSAGE;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;

@Component
public class LookupFieldValueValidator implements CustomFieldValueValidator {

  private static final String INVALID_UUID_MESSAGE = "Field with type %s must contain a valid UUID: %s";

  @Override
  public void validate(Object fieldValue, CustomField fieldDefinition) {
    Type type = fieldDefinition.getType();

    if (Boolean.TRUE.equals(fieldDefinition.getIsRepeatable())) {
      isTrue(fieldValue instanceof List, EXPECT_ARRAY_MESSAGE, type);
      ((List<?>) fieldValue).forEach(value -> validateUuid(value, type));
    } else {
      isTrue(!(fieldValue instanceof List), EXPECT_STRING_MESSAGE, type);
      validateUuid(fieldValue, type);
    }
  }

  @Override
  public List<CustomField.Type> supportedTypes() {
    return List.of(Type.LOOKUP);
  }

  private void validateUuid(Object value, Type type) {
    isInstanceOf(String.class, value, EXPECT_STRING_MESSAGE, type);
    String uuid = value.toString();
    try {
      // Format-only check: accept any well-shaped UUID (including nil / non-v4 foreign ids),
      // reject the non-canonical forms UUID.fromString tolerates via a round-trip.
      if (!UUID.fromString(uuid).toString().equalsIgnoreCase(uuid)) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(String.format(INVALID_UUID_MESSAGE, type, uuid));
    }
  }
}