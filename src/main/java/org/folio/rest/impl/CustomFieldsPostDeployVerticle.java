package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.folio.rest.resource.interfaces.PostDeployVerticle;
import org.folio.spring.ApplicationConfig;
import org.folio.spring.SpringContextUtil;

public class CustomFieldsPostDeployVerticle implements PostDeployVerticle {

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    vertx.executeBlocking(() -> {
        SpringContextUtil.init(vertx, context, ApplicationConfig.class);
        return null;
      })
      .onSuccess(v -> handler.handle(Future.succeededFuture(true)))
      .onFailure(error -> handler.handle(Future.failedFuture(error)));
  }

}
