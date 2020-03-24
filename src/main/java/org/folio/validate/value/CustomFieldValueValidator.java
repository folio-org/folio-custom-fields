package org.folio.validate.value;

import java.util.List;

import org.folio.rest.jaxrs.model.CustomField;

public interface CustomFieldValueValidator {

  /**
   * Validates custom field value
   *
   * @param fieldValue      object that was parsed from json, type of object is String or List<String>
   * @param fieldDefinition field definition that will be used to validate value
   * @throws IllegalArgumentException if validation fails
   */
  void validate(Object fieldValue, CustomField fieldDefinition);

  /**
   * @return List of custom field types that can be processed by this validator
   */
  List<CustomField.Type> supportedTypes();
}
