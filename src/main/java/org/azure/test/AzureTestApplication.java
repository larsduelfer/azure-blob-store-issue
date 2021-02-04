package org.azure.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.TimeZone;

@SpringBootApplication
public class AzureTestApplication {

  public static void main(final String[] args) {
    TimeZone timeZone = TimeZone.getTimeZone("UTC");
    TimeZone.setDefault(timeZone);

    ConfigurableApplicationContext applicationContext =
        SpringApplication.run(AzureTestApplication.class, args);

    // Send the ContextStartedEvent to ApplicationListener implementing classes
    applicationContext.start();
  }
}
