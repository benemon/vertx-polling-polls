package com.redhat.ukiservices.service;

import com.redhat.ukiservices.common.CommonConstants;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class OpinionBeeCompanyVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger("OpinionBeeCompanyVerticle");

	private static final String LOG_REQ_RECV = "Request received on address %s";

	private WebClient client;

	private String apiKey;
	private String opinionBeeBaseUrl;

	public OpinionBeeCompanyVerticle() {
		apiKey = System.getenv(CommonConstants.OB_API_KEY_ENV);
		opinionBeeBaseUrl = System.getenv(CommonConstants.OB_BASE_URL_ENV) != null
				? System.getenv(CommonConstants.OB_BASE_URL_ENV) : CommonConstants.OB_DEFAULT_URL;
	}

	@Override
	public void start() throws Exception {
		super.start();

		MessageConsumer<JsonObject> ebConsumer = vertx.eventBus()
				.consumer(CommonConstants.VERTX_EVENT_BUS_ADDRESS_COMPANIES);

		ebConsumer.handler(payload -> {

			log.info(String.format(LOG_REQ_RECV, CommonConstants.VERTX_EVENT_BUS_ADDRESS_COMPANIES));
			
			this.getCompanies(payload);

		});
	}

	private void getCompanies(Message<JsonObject> message) {
		client = WebClient.create(vertx);

		HttpRequest<JsonObject> companiesRequest = client.get(opinionBeeBaseUrl, "/json/v1.0/companies")
				.addQueryParam("key", apiKey).as(BodyCodec.jsonObject());

		companiesRequest.send(pr -> {
			if (pr.failed()) {
				log.error(pr.cause());
			} else {
				message.reply(pr.result().body());
			}
		});
	}

}
