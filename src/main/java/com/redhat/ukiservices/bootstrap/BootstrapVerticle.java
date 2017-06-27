package com.redhat.ukiservices.bootstrap;

import com.redhat.ukiservices.gateway.GatewayVerticle;
import com.redhat.ukiservices.service.OpinionBeeCompanyVerticle;
import com.redhat.ukiservices.service.OpinionBeePollVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BootstrapVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger("BootstrapVerticle");

	@Override
	public void start() throws Exception {

		super.start();

		vertx.deployVerticle(GatewayVerticle.class.getName(), res -> {
			if (res.failed()) {
				log.error("Initialisation failed", res.cause());
			}

		});

		vertx.deployVerticle(OpinionBeeCompanyVerticle.class.getName(), new DeploymentOptions().setWorker(true),
				res -> {
					if (res.failed()) {
						log.error("Initialisation failed", res.cause());
					}

				});

		vertx.deployVerticle(OpinionBeePollVerticle.class.getName(), new DeploymentOptions().setWorker(true), res -> {
			if (res.failed()) {
				log.error("Initialisation failed", res.cause());
			}

		});
	}

}
