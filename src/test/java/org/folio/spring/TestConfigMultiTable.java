package org.folio.spring;

import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import org.folio.service.RecordService;
import org.folio.service.RecordServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TestConfigSingleTable.class)
public class TestConfigMultiTable {

  @Bean
  public RecordService recordService(Vertx vertx) {
    return RecordServiceImpl.createForMultipleTables(
      vertx,
      Map.of(
        "entityType1",
        List.of("table1", "templates"),
        "entityType2",
        List.of("table2", "templates")));
  }
}
