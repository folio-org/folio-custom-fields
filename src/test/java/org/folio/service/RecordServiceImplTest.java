package org.folio.service;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.CustomFieldsTestUtil.CUSTOM_FIELDS_PATH;
import static org.folio.repository.CustomFieldsConstants.CUSTOM_FIELDS_TABLE;
import static org.folio.service.RecordServiceImplTest.CustomFieldAssert.assertThatCustomField;
import static org.folio.service.RecordServiceImplTest.CustomFieldAssert.assertThatCustomFieldOf;
import static org.folio.service.RecordServiceImplTest.CustomFieldsAssert.assertThatCustomFieldsOf;

import com.google.common.collect.Streams;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import io.vertx.core.internal.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.sqlclient.Tuple;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomField.Type;
import org.folio.rest.jaxrs.model.CustomFieldOptionStatistic;
import org.folio.rest.jaxrs.model.CustomFieldStatistic;
import org.folio.rest.jaxrs.model.PutCustomFieldCollection;
import org.folio.rest.jaxrs.model.SelectField;
import org.folio.rest.jaxrs.model.SelectFieldOption;
import org.folio.rest.jaxrs.model.SelectFieldOptions;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.spring.SpringContextUtil;
import org.folio.spring.TestConfigMultiTable;
import org.folio.spring.TestConfigSingleTable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class RecordServiceImplTest {

  private static final String TENANT = "testtenant";
  private static final String HOST = "http://localhost";
  private static final int port = NetworkUtils.nextFreePort();
  private static final Vertx vertx = Vertx.vertx();
  private static PostgresClient pgClient;
  private static List<CustomField> customFieldsType1;
  private static List<CustomField> customFieldsType3;
  private static List<JsonObject> entitiesType1;
  private static List<JsonObject> entitiesType2;
  private static List<JsonObject> entitiesTemplates;

  @BeforeClass
  public static void beforeClass(TestContext context) {
    String baseURI = HOST + ":" + port;
    RestAssured.requestSpecification =
      new RequestSpecBuilder()
        .setBaseUri(baseURI)
        .addHeader("X-Okapi-Tenant", TENANT)
        .addHeader("X-Okapi-Token", "OKAPI_TOKEN")
        .addHeader("X-Okapi-Url", "http://localhost/okapi")
        .build();

    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    PostgresClient.getInstance(vertx).startPostgresTester();
    deployRestVerticle()
      .onComplete(
        v -> {
          SpringContextUtil.init(
            vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
          pgClient = PostgresClient.getInstance(vertx, TENANT);
        })
      .compose(v -> createTables())
      .onComplete(context.asyncAssertSuccess());
  }

  @AfterClass
  public static void afterClass(TestContext context) {
    RestAssured.reset();
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  private static List<JsonObject> createEntities(String refIdSuffix) {
    JsonObject entity1 =
      new JsonObject()
        .put("id", UUID.randomUUID().toString())
        .put("name", "entity1")
        .put(
          "customFields",
          new JsonObject()
            .put("textbox" + refIdSuffix, "text1")
            .put("singleselect" + refIdSuffix, "opt_1")
            .put("multiselect" + refIdSuffix, new JsonArray().add("opt_1").add("opt_2")));
    JsonObject entity2 =
      new JsonObject()
        .put("id", UUID.randomUUID().toString())
        .put("name", "entity2")
        .put(
          "customFields",
          new JsonObject()
            .put("textbox" + refIdSuffix, "text2")
            .put("singleselect" + refIdSuffix, "opt_2")
            .put("multiselect" + refIdSuffix, new JsonArray().add("opt_2").add("opt_3")));
    return List.of(entity1, entity2);
  }

  private static JsonObject createTemplateEntity() {
    return new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("name", "template1")
      .put(
        "customFields",
        new JsonObject()
          .put("textbox", "text1")
          .put("singleselect", "opt_1")
          .put("multiselect", new JsonArray().add("opt_1").add("opt_2"))
          .put("textbox_2", "text2")
          .put("singleselect_2", "opt_2")
          .put("multiselect_2", new JsonArray().add("opt_2").add("opt_3")));
  }

  private static Future<Void> populateWithEntities(String tableName, List<JsonObject> entities) {
    return Future.all(
        entities.stream()
          .map(
            entity ->
              pgClient.execute(
                "INSERT INTO " + tableName + " (id, jsonb) VALUES ($1, $2)",
                Tuple.of(entity.getString("id"), entity)))
          .toList())
      .mapEmpty();
  }

  private static Future<Void> createTables() {
    return Future.all(
        Stream.of("table1", "table2", "templates")
          .map(
            tableName ->
              pgClient.execute(
                "CREATE TABLE IF NOT EXISTS "
                  + tableName
                  + " (id UUID PRIMARY KEY, jsonb JSONB)"))
          .toList())
      .mapEmpty();
  }

  private static Future<Void> populateTables() {
    return Future.all(
        populateWithEntities("table1", entitiesType1),
        populateWithEntities("table2", entitiesType2),
        populateWithEntities("templates", entitiesTemplates))
      .mapEmpty();
  }

  private static Future<Void> clearTables() {
    return Future.all(
        Stream.of("table1", "table2", "templates", CUSTOM_FIELDS_TABLE)
          .map(tableName -> pgClient.execute("TRUNCATE TABLE " + tableName))
          .toList())
      .mapEmpty();
  }

  private static Future<HttpResponse<Buffer>> deployRestVerticle() {
    return vertx
      .deployVerticle(
        RestVerticle.class.getName(),
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)))
      .compose(
        id -> {
          TenantClient tenantClient =
            new TenantClient(
              HOST + ":" + port, TENANT, "TEST_OKAPI_TOKEN", vertx.createHttpClient());
          TenantAttributes tenantAttributes = new TenantAttributes().withModuleTo("mod-1.0.0");
          return tenantClient
            .postTenant(tenantAttributes)
            .compose(
              response -> {
                String jobId = response.bodyAsJson(TenantJob.class).getId();
                return tenantClient.getTenantByOperationId(jobId, 60000);
              });
        });
  }

  private static Context getFirstContextFromDeployments() {
    return ((VertxInternal) vertx).deploymentManager().deployments().stream()
      .map(deploymentContext -> deploymentContext.deployment().contexts())
      .flatMap(Collection::stream)
      .findFirst()
      .orElseThrow();
  }

  @Before
  public void setUp(TestContext context) {
    entitiesType1 = createEntities("");
    entitiesType2 = createEntities("_2");
    entitiesTemplates = List.of(createTemplateEntity());

    clearTables().compose(v -> populateTables()).onComplete(context.asyncAssertSuccess());

    customFieldsType1 = postCustomFields(createCustomFieldsForEntityType("entityType1"));
    postCustomFields(createCustomFieldsForEntityType("entityType2"));
    customFieldsType3 = postCustomFields(createCustomFieldsForEntityType("entityType3"));
  }

  @Test
  public void testDeleteCustomFieldSingleTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    deleteCustomField(customFieldsType1.getFirst());

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldsOf(res.get(0))
              .allSatisfy(
                cf -> assertThatCustomField(cf).hasSize(2).doesNotContainKey("textbox"));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThat(res.get(2)).containsExactlyInAnyOrderElementsOf(entitiesTemplates);
          }));
  }

  @Test
  public void testDeleteCustomFieldUnused(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    deleteCustomField(customFieldsType3.get(0));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res ->
            assertThat(res)
              .containsExactlyInAnyOrderElementsOf(
                List.of(entitiesType1, entitiesType2, entitiesTemplates))));
  }

  @Test
  public void testPutByIdRemoveSingleOptionSingleTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    customFieldsType1.get(1).getSelectField().getOptions().getValues().remove(1);
    putCustomField(customFieldsType1.get(1));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldOf(res.get(0).get(0))
              .hasSize(2)
              .doesNotContainKey("singleselect");
            assertThat(res.get(0).get(1)).isEqualTo(entitiesType1.get(1));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThat(res.get(2)).containsExactlyInAnyOrderElementsOf(entitiesTemplates);
          }));
  }

  @Test
  public void testPutByIdRemoveSingleOptionUnused(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    customFieldsType3.get(1).getSelectField().getOptions().getValues().remove(1);
    putCustomField(customFieldsType3.get(1));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res ->
            assertThat(res)
              .containsExactlyInAnyOrderElementsOf(
                List.of(entitiesType1, entitiesType2, entitiesTemplates))));
  }

  @Test
  public void testPutByIdRemoveSingleOptionMultiTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigMultiTable.class);
    // remove singleselect opt_1
    customFieldsType1.get(1).getSelectField().getOptions().getValues().remove(1);
    putCustomField(customFieldsType1.get(1));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldOf(res.get(0).get(0))
              .hasSize(2)
              .doesNotContainKey("singleselect");
            assertThat(res.get(0).get(1)).isEqualTo(entitiesType1.get(1));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThatCustomFieldOf(res.get(2).get(0))
              .hasSize(5)
              .doesNotContainKey("singleselect");
          }));
  }

  @Test
  public void testDeleteCustomFieldMultiTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigMultiTable.class);
    deleteCustomField(customFieldsType1.get(0));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldsOf(res.get(0))
              .allSatisfy(
                cf -> assertThatCustomField(cf).hasSize(2).doesNotContainKey("textbox"));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThatCustomFieldOf(res.get(2).get(0))
              .hasSize(5)
              .doesNotContainKey("textbox");
          }));
  }

  @Test
  public void testPutByIdRemoveMultipleOptionsSingleTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);

    customFieldsType1
      .get(2)
      .getSelectField()
      .getOptions()
      .getValues()
      .removeIf(sfo -> List.of("opt_1", "opt_2").contains(sfo.getId()));
    putCustomField(customFieldsType1.get(2));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldOf(res.get(0).get(0))
              .hasSize(2)
              .doesNotContainKey("multiselect");
            assertThatCustomFieldOf(res.get(0).get(1))
              .hasSize(3)
              .containsKeyWithValue("multiselect", new JsonArray().add("opt_3"));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThat(res.get(2)).containsExactlyInAnyOrderElementsOf(entitiesTemplates);
          }));
  }

  @Test
  public void testPutByIdRemoveMultipleOptionsMultiTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigMultiTable.class);

    customFieldsType1
      .get(2)
      .getSelectField()
      .getOptions()
      .getValues()
      .removeIf(sfo -> List.of("opt_1", "opt_2").contains(sfo.getId()));
    putCustomField(customFieldsType1.get(2));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldOf(res.get(0).get(0))
              .hasSize(2)
              .doesNotContainKey("multiselect");
            assertThatCustomFieldOf(res.get(0).get(1))
              .hasSize(3)
              .containsKeyWithValue("multiselect", new JsonArray().add("opt_3"));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThatCustomFieldOf(res.get(2).get(0))
              .hasSize(5)
              .doesNotContainKey("multiselect");
          }));
  }

  @Test
  public void testPutByIdRemoveMultipleOptionsUnused(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);

    customFieldsType3
      .get(2)
      .getSelectField()
      .getOptions()
      .getValues()
      .removeIf(sfo -> List.of("opt_1", "opt_2").contains(sfo.getId()));
    putCustomField(customFieldsType3.get(2));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res ->
            assertThat(res)
              .containsExactlyInAnyOrderElementsOf(
                List.of(entitiesType1, entitiesType2, entitiesTemplates))));
  }

  @Test
  public void testPutPutCustomFieldCollectionMultiTable(TestContext context) {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigMultiTable.class);

    customFieldsType1.get(1).getSelectField().getOptions().getValues().remove(1);
    customFieldsType1
      .get(2)
      .getSelectField()
      .getOptions()
      .getValues()
      .removeIf(sfo -> List.of("opt_1", "opt_2").contains(sfo.getId()));
    customFieldsType1.remove(0);
    putPutCustomFieldCollection(
      new PutCustomFieldCollection()
        .withCustomFields(customFieldsType1)
        .withEntityType("entityType1"));

    fetchAllEntities()
      .onComplete(
        context.asyncAssertSuccess(
          res -> {
            assertThatCustomFieldOf(res.get(0).get(0))
              .hasSize(0)
              .doesNotContainKey("textbox")
              .doesNotContainKey("singleselect")
              .doesNotContainKey("multiselect");
            assertThatCustomFieldOf(res.get(0).get(1))
              .hasSize(2)
              .containsKeyWithValue("multiselect", new JsonArray().add("opt_3"));
            assertThat(res.get(1)).containsExactlyInAnyOrderElementsOf(entitiesType2);
            assertThatCustomFieldOf(res.get(2).get(0))
              .hasSize(3)
              .doesNotContainKey("textbox")
              .doesNotContainKey("singleselect")
              .doesNotContainKey("multiselect");
          }));
  }

  @Test
  public void testRetrieveStatisticSingleTable() {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    CustomFieldStatistic customFieldStatistic = getCustomFieldStatistic(customFieldsType1.get(0));
    assertThat(customFieldStatistic.getCount()).isEqualTo(2);
  }

  @Test
  public void testRetrieveStatisticMultiTable() {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigMultiTable.class);
    CustomFieldStatistic customFieldStatistic = getCustomFieldStatistic(customFieldsType1.get(0));
    assertThat(customFieldStatistic.getCount()).isEqualTo(3);
  }

  @Test
  public void testRetrieveStatisticUnused() {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    CustomFieldStatistic customFieldStatistic = getCustomFieldStatistic(customFieldsType3.get(0));
    assertThat(customFieldStatistic.getCount()).isZero();
  }

  @Test
  public void testRetrieveOptionStatisticSingleTable() {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    CustomFieldOptionStatistic customFieldOptionStatistic =
      getCustomFieldOptionStatistic(customFieldsType1.get(1), "opt_1");
    CustomFieldOptionStatistic customFieldOptionStatistic2 =
      getCustomFieldOptionStatistic(customFieldsType1.get(2), "opt_2");
    assertThat(customFieldOptionStatistic.getCount()).isEqualTo(1);
    assertThat(customFieldOptionStatistic2.getCount()).isEqualTo(2);
  }

  @Test
  public void testRetrieveOptionStatisticMultiTable() {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigMultiTable.class);
    CustomFieldOptionStatistic customFieldOptionStatistic =
      getCustomFieldOptionStatistic(customFieldsType1.get(1), "opt_1");
    CustomFieldOptionStatistic customFieldOptionStatistic2 =
      getCustomFieldOptionStatistic(customFieldsType1.get(2), "opt_2");
    assertThat(customFieldOptionStatistic.getCount()).isEqualTo(2);
    assertThat(customFieldOptionStatistic2.getCount()).isEqualTo(3);
  }

  @Test
  public void testRetrieveOptionStatisticUnused() {
    SpringContextUtil.init(vertx, getFirstContextFromDeployments(), TestConfigSingleTable.class);
    CustomFieldOptionStatistic customFieldOptionStatistic =
      getCustomFieldOptionStatistic(customFieldsType3.get(1), "opt_1");
    assertThat(customFieldOptionStatistic.getCount()).isZero();
  }

  private Future<List<List<JsonObject>>> fetchAllEntities() {
    List<Future<List<JsonObject>>> futures =
      Stream.of("table1", "table2", "templates").map(this::getEntitiesFromTable).toList();
    return Future.all(futures).map(CompositeFuture::list);
  }

  private List<CustomField> postCustomFields(List<CustomField> customFields) {
    return customFields.stream()
      .map(
        customField ->
          given()
            .header("Content-Type", "application/json")
            .body(customField)
            .post(CUSTOM_FIELDS_PATH)
            .then()
            .statusCode(201)
            .extract()
            .as(CustomField.class))
      .collect(Collectors.toList());
  }

  private void putCustomField(CustomField customField) {
    given()
      .header("Content-Type", "application/json")
      .body(customField)
      .put(CUSTOM_FIELDS_PATH + "/{id}", customField.getId())
      .then()
      .statusCode(204);
  }

  private CustomFieldStatistic getCustomFieldStatistic(CustomField customField) {
    return given()
      .get(CUSTOM_FIELDS_PATH + "/{id}/stats", customField.getId())
      .then()
      .statusCode(200)
      .extract()
      .as(CustomFieldStatistic.class);
  }

  private CustomFieldOptionStatistic getCustomFieldOptionStatistic(
    CustomField customField, String optId) {
    return given()
      .get(CUSTOM_FIELDS_PATH + "/{id}/options/{optId}/stats", customField.getId(), optId)
      .then()
      .statusCode(200)
      .extract()
      .as(CustomFieldOptionStatistic.class);
  }

  private void putPutCustomFieldCollection(PutCustomFieldCollection putCustomFieldCollection) {
    given()
      .header("Content-Type", "application/json")
      .body(putCustomFieldCollection)
      .put(CUSTOM_FIELDS_PATH)
      .then()
      .statusCode(204);
  }

  private void deleteCustomField(CustomField customField) {
    given().delete(CUSTOM_FIELDS_PATH + "/{id}", customField.getId()).then().statusCode(204);
  }

  private List<CustomField> createCustomFieldsForEntityType(String entityType) {
    CustomField textbox =
      new CustomField()
        .withId(UUID.randomUUID().toString())
        .withName("textbox")
        .withType(Type.TEXTBOX_SHORT)
        .withEntityType(entityType);
    CustomField singleselect =
      new CustomField()
        .withId(UUID.randomUUID().toString())
        .withName("singleselect")
        .withType(Type.SINGLE_SELECT_DROPDOWN)
        .withSelectField(
          new SelectField()
            .withMultiSelect(false)
            .withOptions(
              new SelectFieldOptions()
                .withValues(
                  Arrays.asList(
                    new SelectFieldOption().withId("opt_0").withValue("opt0"),
                    new SelectFieldOption().withId("opt_1").withValue("opt1"),
                    new SelectFieldOption().withId("opt_2").withValue("opt2")))))
        .withEntityType(entityType);
    CustomField multiselect =
      new CustomField()
        .withId(UUID.randomUUID().toString())
        .withName("multiselect")
        .withType(Type.MULTI_SELECT_DROPDOWN)
        .withSelectField(
          new SelectField()
            .withMultiSelect(true)
            .withOptions(
              new SelectFieldOptions()
                .withValues(
                  Arrays.asList(
                    new SelectFieldOption().withId("opt_0").withValue("opt0"),
                    new SelectFieldOption().withId("opt_1").withValue("opt1"),
                    new SelectFieldOption().withId("opt_2").withValue("opt2"),
                    new SelectFieldOption().withId("opt_3").withValue("opt3")))))
        .withEntityType(entityType);
    return List.of(textbox, singleselect, multiselect);
  }

  private Future<List<JsonObject>> getEntitiesFromTable(String tableName) {
    return pgClient
      .execute("SELECT jsonb FROM " + tableName + " ORDER BY jsonb->'name'")
      .map(rs -> Streams.stream(rs.iterator()).map(row -> row.getJsonObject("jsonb")).toList());
  }

  static class CustomFieldsAssert extends AbstractAssert<CustomFieldsAssert, List<JsonObject>> {

    protected CustomFieldsAssert(List<JsonObject> json) {
      super(json, CustomFieldsAssert.class);
    }

    public static ListAssert<JsonObject> assertThatCustomFieldsOf(List<JsonObject> actual) {
      return new ListAssert<>(
        actual.stream().map(json -> json.getJsonObject("customFields")).toList());
    }
  }

  static class CustomFieldAssert extends AbstractAssert<CustomFieldAssert, JsonObject> {

    protected CustomFieldAssert(JsonObject actual) {
      super(actual, CustomFieldAssert.class);
    }

    public static CustomFieldAssert assertThatCustomFieldOf(JsonObject actual) {
      return new CustomFieldAssert(actual.getJsonObject("customFields"));
    }

    public static CustomFieldAssert assertThatCustomField(JsonObject actual) {
      return new CustomFieldAssert(actual);
    }

    public CustomFieldAssert hasSize(int size) {
      assertThat(actual).hasSize(size);
      return this;
    }

    public CustomFieldAssert containsKeyWithValue(String key, Object value) {
      assertThat(actual.getValue(key)).isEqualTo(value);
      return this;
    }

    public CustomFieldAssert doesNotContainKey(String key) {
      assertThat(actual.containsKey(key)).isFalse();
      return this;
    }
  }
}
