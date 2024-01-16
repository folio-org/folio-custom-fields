package org.folio.validate.value;

import static org.apache.commons.lang3.Validate.isInstanceOf;
import static org.folio.validate.value.CustomFieldValueValidatorConstants.EXPECT_STRING_MESSAGE;

import java.util.List;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.folio.validate.value.format.DateFormatValidator;
import org.springframework.stereotype.Component;

@Component
public class DatePickerValueValidator implements CustomFieldValueValidator {

  private static final DateFormatValidator dateFormatValidator = new DateFormatValidator();

  @Override
  public void validate(Object fieldValue, CustomField fieldDefinition) {
    isInstanceOf(String.class, fieldValue, EXPECT_STRING_MESSAGE, fieldDefinition.getType());
    dateFormatValidator.validate(fieldValue.toString());
  }

  @Override
  public List<CustomField.Type> supportedTypes() {
    return List.of(Type.DATE_PICKER);
  }
}
