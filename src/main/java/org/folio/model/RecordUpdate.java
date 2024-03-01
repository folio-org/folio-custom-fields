package org.folio.model;

import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.folio.rest.jaxrs.model.CustomField;

@Value
public class RecordUpdate {

  @NonNull CustomField customField;
  @NonNull List<String> optionIdsToDelete;
  @NonNull List<String> defaultIds;

  /**
   * Method returns refId from customField. Added for compatibility with previous version.
   *
   * @return refId
   */
  public String getRefId() {
    return customField.getRefId();
  }
}
