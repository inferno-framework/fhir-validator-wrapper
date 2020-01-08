package org.mitre.inferno.utils;

import org.eclipse.jetty.server.CustomRequestLog;
import org.slf4j.Logger;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;

public class SparkUtils {
  public static void createServerWithRequestLog(Logger logger) {
    EmbeddedJettyFactory factory = createEmbeddedJettyFactoryWithRequestLog(logger);
    EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, factory);
  }

  private static EmbeddedJettyFactory createEmbeddedJettyFactoryWithRequestLog(Logger logger) {
    CustomRequestLog requestLog = new RequestLogFactory(logger).getLog();
    return new EmbeddedJettyFactoryConstructor(requestLog).create();
  }
}
