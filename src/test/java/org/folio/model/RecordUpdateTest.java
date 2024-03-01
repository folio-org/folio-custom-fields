package org.folio.model;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import org.folio.rest.jaxrs.model.CustomField;
import org.junit.Test;

public class RecordUpdateTest {

  @Test
  public void testGetRefId() {
    RecordUpdate recordUpdate = new RecordUpdate(new CustomField().withRefId("textbox"), emptyList(), emptyList());
    assertEquals("textbox", recordUpdate.getRefId());
  }
}
