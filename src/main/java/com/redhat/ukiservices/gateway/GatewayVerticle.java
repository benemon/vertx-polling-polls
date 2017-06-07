package com.redhat.ukiservices.gateway;

import com.redhat.ukiservices.common.CommonConstants;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.StaticHandler;

public class GatewayVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger("GatewayVerticle");

	private boolean online;
	private WebClient client;

	private String apiKey;
	private String opinionBeeBaseUrl;

	public GatewayVerticle() {
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

		HttpServer server = vertx.createHttpServer().requestHandler(router::accept)
				.listen(config().getInteger("http.port", 8080), ar -> {
					online = ar.succeeded();
				});
	}

	private void getCompanies(RoutingContext rc) {

		JsonObject payload = new JsonObject();
		payload.put("method", rc.request().rawMethod());

		vertx.eventBus().send(CommonConstants.VERTX_EVENT_BUS_ADDRESS_COMPANIES, payload, ar -> {
			if (ar.succeeded()) {
				JsonObject job = (JsonObject) ar.result().body();
				rc.response().end(job.encodePrettily());
			} else {
				rc.response().setStatusCode(500);
			}
		});
	}

	private void getPollData(RoutingContext rc) {

		JsonObject payload = new JsonObject();
		payload.put("method", rc.request().rawMethod());
		payload.put("company", rc.request().getParam("company"));
		payload.put("pollType", rc.request().getParam("pollType"));
		payload.put("limit", rc.request().getParam("limit"));

		vertx.eventBus().send(CommonConstants.VERTX_EVENT_BUS_ADDRESS_POLLS, payload, ar -> {
			if (ar.succeeded()) {
				JsonArray job = (JsonArray) ar.result().body();
				rc.response().end(job.encodePrettily());
			} else {
				rc.response().setStatusCode(500);
			}
		});

	}

}
