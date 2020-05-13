package org.mitre.inferno;

import org.mitre.inferno.rest.ValidatorEndpoint;
import org.mitre.inferno.utils.SparkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

  /**
   * Starting point for the Validation Service.
   * <p>Passing the 'prepare' argument causes the FHIR artifacts needed to be downloaded.</p>
   * @param args the application initialization arguments
   */
  public static void main(String[] args) {
    Logger logger = LoggerFactory.getLogger(App.class);
    if (args.length > 0) {
      if (args[0].equals("prepare")) {
        logger.info("Initializing Validator App...");
        initializeValidator();
      } else {
        logger.warn("Argument " + args[0] + " is unknown");
        startApp();
      }
    } else {
      startApp();
    }
  }

  /**
   * Only used for getting the FHIR artifacts cached.
   */
  private static void initializeValidator() {
    new Validator("./igs/package");
  }

  /**
   * Starts the app.
   */
  private static void startApp() {
    Logger logger = LoggerFactory.getLogger(App.class);
    logger.info("Starting Server...");
    SparkUtils.createServerWithRequestLog(logger);
    ValidatorEndpoint.getInstance(getPortNumber());
  }

  private static int getPortNumber() {
    if (System.getenv("VALIDATOR_PORT") != null) {
      return Integer.parseInt(System.getenv("VALIDATOR_PORT"));
    } else {
      return 4567;
    }
  }
}
