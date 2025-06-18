package org.folio.validate.definition;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.Validate.isTrue;

import java.util.Map;
import java.util.Set;

import org.folio.rest.jaxrs.model.CustomField;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Component
public class DisplayInAccordionValidator implements Validatable {

  private final Map<String, Set<CustomField.DisplayInAccordion>> allowedValuesByTypeMap =
    ImmutableMap.<String, Set<CustomField.DisplayInAccordion>>builder()
      .put("user", ImmutableSet.<CustomField.DisplayInAccordion>builder()
        .add(CustomField.DisplayInAccordion.USER_INFORMATION)
        .add(CustomField.DisplayInAccordion.EXTENDED_INFORMATION)
        .add(CustomField.DisplayInAccordion.CONTACT_INFORMATION)
        .add(CustomField.DisplayInAccordion.DEFAULT)
        .add(CustomField.DisplayInAccordion.FEES_FINES)
        .add(CustomField.DisplayInAccordion.LOANS)
        .add(CustomField.DisplayInAccordion.REQUESTS)
        .build())
      .build();

  @Override
  public void validateDefinition(CustomField fieldDefinition) {
    var value = fieldDefinition.getDisplayInAccordion();
    var entityType = fieldDefinition.getEntityType();
    var allowedValues = allowedValuesByTypeMap.getOrDefault(entityType, emptySet());
    isTrue(allowedValues.contains(value),
      "'%s' value is not allowed for displayInAccordion, " +
        "only the following values are allowed for entity type '%s': %s",
      value.value(), entityType, allowedValues);
  }

  @Override
  public boolean isApplicable(CustomField fieldDefinition) {
    return fieldDefinition.getDisplayInAccordion() != null;
  }
}
