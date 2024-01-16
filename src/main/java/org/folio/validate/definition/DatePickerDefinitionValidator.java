package org.folio.validate.definition;

import static org.folio.validate.definition.AllowedFieldsConstants.COMMON_ALLOWED_FIELDS;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.springframework.stereotype.Component;

@Component
public class DatePickerDefinitionValidator implements Validatable {

  @Override
  public void validateDefinition(CustomField fieldDefinition) {
    CustomDefinitionValidationUtil.onlyHasAllowedFields(fieldDefinition, COMMON_ALLOWED_FIELDS);
  }

  @Override
  public boolean isApplicable(CustomField fieldDefinition) {
    return Type.DATE_PICKER.equals(fieldDefinition.getType());
  }
}
