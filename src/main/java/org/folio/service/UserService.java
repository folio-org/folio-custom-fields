package org.folio.service;

import java.util.Map;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.model.User;
import org.folio.okapi.common.XOkapiHeaders;

@Component
public class UserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

  private static final String USERS_ENDPOINT_TEMPLATE = "/users/%s";

  private static final String AUTHORIZATION_FAILURE_MESSAGE = "Authorization failure";
  private static final String USER_NOT_FOUND_MESSAGE = "User not found";
  private static final String CANNOT_GET_USER_DATA_MESSAGE = "Cannot get user data: %s";

  private final WebClient webClient;

  public UserService(@Autowired Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  /**
   * Returns the user information for the userid specified in the x-okapi-token header.
   *
   * @param okapiHeaders The headers for the current API call.
   * @return User information based on userid from header.
   */
  public Future<User> getUserInfo(final Map<String, String> okapiHeaders) {
    MultiMap headers = new HeadersMultiMap();
    headers.addAll(okapiHeaders);

    Promise<HttpResponse<User>> promise = Promise.promise();
    String userId = okapiHeaders.get(XOkapiHeaders.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      String usersPath = String.format(USERS_ENDPOINT_TEMPLATE, userId);
      webClient.getAbs(headers.get(XOkapiHeaders.URL) + usersPath)
        .putHeaders(headers)
        .as(BodyCodec.json(User.class))
        .expect(ResponsePredicate.create(ResponsePredicate.SC_OK, errorConverter()))
        .send(promise);
      return promise.future().map(HttpResponse::body);
    } else {
      return Future.failedFuture(new NotAuthorizedException(XOkapiHeaders.USER_ID + " header is required"));
    }
  }

  private ErrorConverter errorConverter() {
    return ErrorConverter.createFullBody(result -> {
      HttpResponse<Buffer> response = result.response();
      if (response.statusCode() == 401 || response.statusCode() == 403) {
        LOGGER.error(AUTHORIZATION_FAILURE_MESSAGE);
        throw new NotAuthorizedException(AUTHORIZATION_FAILURE_MESSAGE);
      } else if (response.statusCode() == 404) {
        LOGGER.error(USER_NOT_FOUND_MESSAGE);
        throw new NotFoundException(USER_NOT_FOUND_MESSAGE);
      } else {
        String message = result.message();
        String msg = String.format(CANNOT_GET_USER_DATA_MESSAGE, message);
        LOGGER.error(msg);
        throw new IllegalStateException(message);
      }
    });
  }
}
