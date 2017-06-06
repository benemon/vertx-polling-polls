package com.redhat.ukiservices.pollingpolls;

import com.redhat.ukiservices.common.CommonConstants;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.StaticHandler;

public class PollPollingVerticle extends AbstractVerticle {

	private boolean online;
	private WebClient client;

	private String apiKey;
	private String opinionBeeBaseUrl;

	public PollPollingVerticle() {
		apiKey = System.getenv(CommonConstants.OB_API_KEY_ENV);
		opinionBeeBaseUrl = System.getenv(CommonConstants.OB_BASE_URL_ENV) != null
				? System.getenv(CommonConstants.OB_BASE_URL_ENV) : CommonConstants.OB_DEFAULT_URL;
	}

	@Override
	public void start() throws Exception {
		super.start();

		Router router = Router.router(vertx);

		HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx).register("server-online",
				fut -> fut.complete(online ? Status.OK() : Status.KO()));

		router.get("/api/health/readiness").handler(rc -> rc.response().end(CommonConstants.OK));
		router.get("/api/health/liveness").handler(healthCheckHandler);
		router.get("/api/polls/:company/:pollType").handler(this::getPollData);
		router.get("/api/companies").handler(this::getCompanies);
		router.get("/*").handler(StaticHandler.create());

		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}

	private void getCompanies(RoutingContext rc) {
		client = WebClient.create(vertx);

		HttpRequest<JsonObject> companiesRequest = client.get(opinionBeeBaseUrl, "/json/v1.0/companies")
				.addQueryParam("key", apiKey)
				.as(BodyCodec.jsonObject());

		companiesRequest.send(pr -> {
			if (pr.failed()) {
				rc.fail(pr.cause());
			} else {
				rc.response().end(pr.result().body().encodePrettily());
			}
		});
	}

	private void getPollData(RoutingContext rc) {
		client = WebClient.create(vertx);

		String pollType = rc.request().getParam("pollType");
		String company = rc.request().getParam("company");
		String limit = rc.request().getParam("limit");

		HttpRequest<Buffer> draftRequest = client.get(opinionBeeBaseUrl, "/json/v1.0/polls")
				.addQueryParam("key", apiKey)
				.addQueryParam("code", pollType)
				.addQueryParam("company", company);

		if (limit != null && limit.length() > 0) {
			draftRequest.addQueryParam("limit", limit);
		}

		HttpRequest<JsonArray> pollRequest = draftRequest.as(BodyCodec.jsonArray());

		pollRequest.send(pr -> {
			if (pr.failed()) {
				rc.fail(pr.cause());
			} else {
				rc.response().end(pr.result().body().encodePrettily());
			}
		});

	}

}
