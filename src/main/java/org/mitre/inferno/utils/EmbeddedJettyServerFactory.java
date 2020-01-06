package org.mitre.inferno.utils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import spark.embeddedserver.jetty.JettyServerFactory;

class EmbeddedJettyServerFactory implements JettyServerFactory {
  private EmbeddedJettyFactoryConstructor embeddedJettyFactoryConstructor;
  
  EmbeddedJettyServerFactory(EmbeddedJettyFactoryConstructor embeddedJettyFactoryConstructor) {
    this.embeddedJettyFactoryConstructor = embeddedJettyFactoryConstructor;
  }
  
  @Override
  public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
    Server server;
    if (maxThreads > 0) {
      int max = maxThreads > 0 ? maxThreads : 200;
      int min = minThreads > 0 ? minThreads : 8;
      int idleTimeout = threadTimeoutMillis > 0 ? threadTimeoutMillis : 60;
      server = new Server(new QueuedThreadPool(max, min, idleTimeout));
    } else {
      server = new Server();
    }
    
    server.setRequestLog(embeddedJettyFactoryConstructor.requestLog);
    return server;
  }
  
  @Override
  public Server create(ThreadPool threadPool) {
    return new Server(threadPool);
  }
}
