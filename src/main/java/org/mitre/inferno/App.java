package org.mitre.inferno;

import org.mitre.inferno.rest.ValidatorEndpoint;

public class App {

  /**
   * Starting point for the Validation Service.
   * <p>Passing the 'prepare' argument causes the FHIR artifacts needed to be downloaded.</p>
   * @param args the application initialization arguments
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      if (args[0].equals("prepare")) {
        System.out.println("Initializing Validator App...");
        initializeValidator();
      } else {
        System.out.println("Argument " + args[0] + " is unknown");
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
    System.out.println("Starting Server...");
    ValidatorEndpoint.getInstance();
  }
}
