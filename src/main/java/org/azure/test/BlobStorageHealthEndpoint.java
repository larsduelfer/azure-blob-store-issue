package org.azure.test;

import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BlobStorageHealthEndpoint implements HealthIndicator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStorageHealthEndpoint.class);

  private final BlobContainerClient blobContainerClient;

  public BlobStorageHealthEndpoint(BlobContainerClient blobContainerClient) {
    this.blobContainerClient = blobContainerClient;
  }

  @Override
  public Health health() {
    Health.Builder builder = Health.up();
    if (!blobContainerClient.exists()) {
      LOGGER.error("Azure storage blob health check cannot find blob container.");
      return Health.down()
          .withDetail("Error Details", "Container not found")
          .build();
    } else {
      return builder.build();
    }
  }
}
