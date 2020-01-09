package org.mitre.inferno.utils;

import java.io.IOException;

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.slf4j.Logger;

public class RequestLogFactory {
  
  private Logger logger;
  private CustomRequestLog crl;
  
  public RequestLogFactory(Logger logger) {
    this.logger = logger;
    this.crl = new CustomRequestLog(create(), CustomRequestLog.EXTENDED_NCSA_FORMAT);
  }
  
  Slf4jRequestLogWriter create() {
    return new Slf4jRequestLogWriter() {
      @Override
      protected boolean isEnabled() {
        return true;
      }
      
      @Override
      public void write(String s) throws IOException {
        logger.info(s);
      }
    };
  }
  
  public CustomRequestLog getLog() {
    return this.crl;
  }
}
