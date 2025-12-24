package org.folio.service;

import static io.vertx.core.http.HttpResponseExpectation.SC_OK;
import static org.folio.HttpStatus.SC_FORBIDDEN;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.HttpStatus.SC_UNAUTHORIZED;

import java.util.Map;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpResponseHead;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.model.User;
import org.folio.okapi.common.XOkapiHeaders;

@Log4j2
@Component
public class UserService {
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
    MultiMap headers = HeadersMultiMap.httpHeaders();
    headers.addAll(MapUtils.emptyIfNull(okapiHeaders));

    log.debug("getUserInfo:: Attempts to get userInfo by [userId]");
    String userId = okapiHeaders.get(XOkapiHeaders.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      String usersPath = String.format(USERS_ENDPOINT_TEMPLATE, userId);
      return webClient.getAbs(headers.get(XOkapiHeaders.URL) + usersPath)
        .putHeaders(headers)
        .as(BodyCodec.json(User.class))
        .send()
        .expecting(SC_OK.wrappingFailure(this::handleError))
        .map(HttpResponse::body);
    } else {
      NotAuthorizedException exception =
        new NotAuthorizedException(XOkapiHeaders.USER_ID + " header is required");
      log.warn("Empty userId, msg:: {}", exception.getMessage());
      return Future.failedFuture(exception);
    }
  }

  private Throwable handleError(HttpResponseHead response, Throwable throwable) {
    switch (response.statusCode()) {
      case SC_UNAUTHORIZED, SC_FORBIDDEN -> {
        log.warn(AUTHORIZATION_FAILURE_MESSAGE);
        return new NotAuthorizedException(AUTHORIZATION_FAILURE_MESSAGE);
      }
      case SC_NOT_FOUND -> {
        log.warn(USER_NOT_FOUND_MESSAGE);
        return new NotFoundException(USER_NOT_FOUND_MESSAGE);
      }
      default -> {
        String msg = String.format(CANNOT_GET_USER_DATA_MESSAGE, throwable.getMessage());
        log.warn(msg);
        return new IllegalStateException(msg);
      }
    }
  }
}
