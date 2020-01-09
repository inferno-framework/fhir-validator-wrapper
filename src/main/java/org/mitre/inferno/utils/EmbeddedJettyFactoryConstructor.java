package org.mitre.inferno.utils;

import org.eclipse.jetty.server.CustomRequestLog;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;

public class EmbeddedJettyFactoryConstructor {
  CustomRequestLog requestLog;

  public EmbeddedJettyFactoryConstructor(CustomRequestLog requestLog) {
    this.requestLog = requestLog;
  }

  EmbeddedJettyFactory create() {
    EmbeddedJettyServerFactory embeddedJettyServerFactory = new EmbeddedJettyServerFactory(this);

    return new EmbeddedJettyFactory(embeddedJettyServerFactory);
  }

}
