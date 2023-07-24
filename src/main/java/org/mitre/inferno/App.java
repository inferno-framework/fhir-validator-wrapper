package org.mitre.inferno;

import java.io.IOException;
import org.mitre.inferno.rest.Endpoints;
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
  private static Validator initializeValidator() {
    Logger logger = LoggerFactory.getLogger(App.class);
    try {
      return new Validator("./igs", areDisplayIssuesWarnings());
    } catch (Exception e) {
      logger.error("There was an error initializing the validator:", e);
      System.exit(1);
      return null; // unreachable
    }
  }

  private static FHIRPathEvaluator initializePathEvaluator() {
    Logger logger = LoggerFactory.getLogger(App.class);
    try {
      return new FHIRPathEvaluator();
    } catch (IOException e) {
      logger.error("There was an error initializing the FHIRPath evaluator:", e);
      System.exit(1);
      return null; // unreachable
    }
  }

  /**
   * Starts the app.
   */
  private static void startApp() {
    Logger logger = LoggerFactory.getLogger(App.class);
    logger.info("Starting Server...");
    SparkUtils.createServerWithRequestLog(logger);
    Endpoints.getInstance(
        initializeValidator(),
        initializePathEvaluator(),
        getPortNumber()
    );
  }

  private static int getPortNumber() {
    String port = System.getenv("VALIDATOR_PORT");
    if (port != null) {
      return Integer.parseInt(port);
    } else {
      return 4567;
    }
  }

  private static boolean areDisplayIssuesWarnings() {
    String displayIssuesAreWarnings = System.getenv("DISPLAY_ISSUES_ARE_WARNINGS");
    if (displayIssuesAreWarnings != null) {
      return Boolean.parseBoolean(displayIssuesAreWarnings);
    } else {
      return false;
    }
  }
}
