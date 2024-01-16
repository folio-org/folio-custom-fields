package org.folio.validate.value.format;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatValidator implements FormatValidator {

  private static final String INVALID_DATE_FORMAT_MESSAGE = "Invalid date format: %s";

  @Override
  public void validate(String value) {
    try {
      DateTimeFormatter.ISO_LOCAL_DATE.parse(value);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(String.format(INVALID_DATE_FORMAT_MESSAGE, value));
    }
  }
}
