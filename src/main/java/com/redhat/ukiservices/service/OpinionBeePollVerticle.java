package com.redhat.ukiservices.service;

import com.redhat.ukiservices.common.CommonConstants;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class OpinionBeePollVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger("OpinionBeePollVerticle");

	private static final String LOG_REQ_RECV = "Request received on address %s";

	private WebClient client;

	private String apiKey;
	private String opinionBeeBaseUrl;

	public OpinionBeePollVerticle() {
		apiKey = System.getenv(CommonConstants.OB_API_KEY_ENV);
		opinionBeeBaseUrl = System.getenv(CommonConstants.OB_BASE_URL_ENV) != null
				? System.getenv(CommonConstants.OB_BASE_URL_ENV) : CommonConstants.OB_DEFAULT_URL;
	}

	@Override
	public void start() throws Exception {
		super.start();

		MessageConsumer<JsonObject> ebConsumer = vertx.eventBus()
				.consumer(CommonConstants.VERTX_EVENT_BUS_ADDRESS_POLLS);

		ebConsumer.handler(payload -> {
			log.info(String.format(LOG_REQ_RECV, CommonConstants.VERTX_EVENT_BUS_ADDRESS_POLLS));

			String pollType = payload.body().getString("pollType");
			String company = payload.body().getString("company");
			String limit = payload.body().getString("limit");

			this.getPollData(pollType, company, limit, payload);

		});

	}

	private void getPollData(String pollType, String company, String limit, Message<JsonObject> message) {
		client = WebClient.create(vertx);

		HttpRequest<Buffer> draftRequest = client.get(opinionBeeBaseUrl, "/json/v1.0/polls")
				.addQueryParam("key", apiKey).addQueryParam("code", pollType).addQueryParam("company", company);
		
		if (limit != null && limit.length() > 0) {
			draftRequest.addQueryParam("limit", limit);
		}

		HttpRequest<JsonArray> pollRequest = draftRequest.as(BodyCodec.jsonArray());

		pollRequest.send(pr -> {
			if (pr.failed()) {
				log.error(pr.cause());
			} else {			
				message.reply(pr.result().body());
			}
		});
	}

}
