package org.folio.validate.definition;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.isTrue;

import static org.folio.validate.definition.AllowedFieldsConstants.LOOKUP_ALLOWED_FIELDS;

import java.util.Objects;

import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;

@Component
public class LookupDefinitionValidator implements Validatable {

  private static final String BLANK_LOOKUP_FIELD_PROP_MESSAGE = "The 'lookupField' property should be defined";
  private static final String BLANK_REF_ENTITY_TYPE_MESSAGE = "The 'refEntityType' property should be defined";

  @Override
  public void validateDefinition(CustomField fieldDefinition) {
    CustomDefinitionValidationUtil.onlyHasAllowedFields(fieldDefinition, LOOKUP_ALLOWED_FIELDS);
    validateLookupFieldDefined(fieldDefinition);
  }

  @Override
  public boolean isApplicable(CustomField fieldDefinition) {
    return Type.LOOKUP.equals(fieldDefinition.getType());
  }

  private void validateLookupFieldDefined(CustomField fieldDefinition) {
    isTrue(Objects.nonNull(fieldDefinition.getLookupField()), BLANK_LOOKUP_FIELD_PROP_MESSAGE);
    isTrue(isNotBlank(fieldDefinition.getLookupField().getRefEntityType()), BLANK_REF_ENTITY_TYPE_MESSAGE);
  }
}