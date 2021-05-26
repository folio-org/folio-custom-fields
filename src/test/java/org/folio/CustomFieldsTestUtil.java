package org.folio;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import static org.folio.repository.CustomFieldsConstants.CUSTOM_FIELDS_TABLE;
import static org.folio.test.util.DBTestUtil.deleteFromTable;
import static org.folio.test.util.DBTestUtil.getAll;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.test.util.TokenTestUtil.createTokenHeader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import io.restassured.http.Header;
import io.vertx.core.Vertx;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.CustomField;

public class CustomFieldsTestUtil {

  public static final String USER1_ID = "11111111-1111-1111-1111-111111111111";
  public static final String USER2_ID = "22222222-2222-2222-2222-222222222222";
  public static final String USER3_ID = "33333333-3333-3333-3333-333333333333";
  public static final String USER4_ID = "44444444-4444-4444-4444-444444444444";

  public static final Header USER1_HEADER = new Header(XOkapiHeaders.USER_ID, USER1_ID);
  public static final Header USER2_HEADER = new Header(XOkapiHeaders.USER_ID, USER2_ID);

  public static final String STUB_FIELD_ID = "11111111-1111-1111-a111-111111111111";
  public static final String CUSTOM_FIELDS_PATH = "/custom-fields";
  public static final String USERS_PATH = "/users";

  private CustomFieldsTestUtil() {
  }

  public static void deleteAllCustomFields(Vertx vertx) {
    deleteFromTable(vertx, CUSTOM_FIELDS_TABLE);
  }

  public static List<CustomField> getAllCustomFields(Vertx vertx) {
    return getAll(CustomField.class, vertx, CUSTOM_FIELDS_TABLE);
  }

  public static void mockUserRequests() throws IOException, URISyntaxException {
    stubFor(
      get(urlPathEqualTo(USERS_PATH + "/" + USER1_ID))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(readFile("users/mock_user_1.json"))
        ));

    stubFor(
      get(urlPathEqualTo(USERS_PATH + "/" + USER2_ID))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(readFile("users/mock_user_2.json"))
        ));

    stubFor(
      get(urlPathEqualTo(USERS_PATH + "/" + USER3_ID))
        .willReturn(aResponse()
          .withStatus(403))
    );

    stubFor(
      get(urlPathEqualTo(USERS_PATH + "/" + USER4_ID))
        .willReturn(aResponse()
          .withStatus(404))
    );
  }

  public static String itemResourcePath(String id) {
    return CUSTOM_FIELDS_PATH + "/" + id;
  }

  public static String itemStatResourcePath(String id) {
    return itemResourcePath(id) + "/stats";
  }

  public static String itemOptionStatResourcePath(String id, String optId) {
    return itemResourcePath(id) + "/options/" + optId + "/stats";
  }
}
