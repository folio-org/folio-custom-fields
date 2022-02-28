package org.folio.service;

import static org.jeasy.random.FieldPredicates.named;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import io.vertx.core.Future;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.jeasy.random.randomizers.misc.UUIDRandomizer;
import org.jeasy.random.randomizers.text.StringDelegatingRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import org.folio.model.RecordUpdate;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFieldOptionStatistic;
import org.folio.rest.jaxrs.model.CustomFieldStatistic;
import org.folio.test.junit.TestStartLoggingRule;

public class NoOpRecordServiceTest {

  private static EasyRandom cfRandom;
  private static StringRandomizer tenantIdRandom;
  @Rule
  public TestRule watcher = TestStartLoggingRule.instance();
  private CustomField field;
  private String tenantId;
  private NoOpRecordService service;

  @BeforeClass
  public static void setUpClass() {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("id"), new StringDelegatingRandomizer(new UUIDRandomizer()))
      .randomize(named("name"), new StringRandomizer(20))
      .randomize(named("refId"), new StringRandomizer(20))
      .randomize(named("entityType"), new StringDelegatingRandomizer(new EnumRandomizer<>(EntityType.class)))
      .excludeField(named("metadata"));

    cfRandom = new EasyRandom(params);

    tenantIdRandom = new StringRandomizer(10);
  }

  private static CustomField nextRandomCustomField() {
    return cfRandom.nextObject(CustomField.class);
  }

  private static String nextRandomTenantId() {
    return tenantIdRandom.getRandomValue();
  }

  @Before
  public void setUp() {
    field = nextRandomCustomField();
    tenantId = nextRandomTenantId();

    service = new NoOpRecordService();
  }

  @Test
  public void shouldReturnEmptyStatistic() {
    Future<CustomFieldStatistic> stat = service.retrieveStatistic(field, tenantId);

    assertNotNull(stat);
    assertTrue(stat.succeeded());
    assertEquals(stat.result(), new CustomFieldStatistic()
      .withFieldId(field.getId())
      .withEntityType(field.getEntityType())
      .withCount(0));
  }

  @Test
  public void shouldReturnEmptyOptionStatistic() {
    String optionId = "opt_0";
    Future<CustomFieldOptionStatistic> stat = service.retrieveOptionStatistic(field, optionId, tenantId);

    assertNotNull(stat);
    assertTrue(stat.succeeded());
    assertEquals(stat.result(), new CustomFieldOptionStatistic()
      .withOptionId(optionId)
      .withCustomFieldId(field.getId())
      .withEntityType(field.getEntityType())
      .withCount(0));
  }

  @Test
  public void shouldReturnSuccessOnDeleteAllValues() {
    Future<Void> res = service.deleteAllValues(field, tenantId);

    assertNotNull(res);
    assertTrue(res.succeeded());
  }

  @Test
  public void shouldReturnSuccessOnDeleteMissedOptionValues() {
    RecordUpdate recordUpdate = new RecordUpdate(field.getRefId(), Collections.emptyList(), Collections.emptyList());
    Future<Void> res = service.deleteMissedOptionValues(recordUpdate, tenantId);

    assertNotNull(res);
    assertTrue(res.succeeded());
  }

  private enum EntityType {

    PROVIDER("provider"),
    PACKAGE("package"),
    TITLE("title"),
    RESOURCE("resource"),
    USER("user"),
    ORDER("order"),
    ORDERLINE("orderline"),
    ITEM("item"),
    REQUEST("request");

    private final String value;

    EntityType(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

  }
}
