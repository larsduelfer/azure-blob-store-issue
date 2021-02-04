package org.azure.test;

import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.implementation.AzureBlobStorageImpl;
import com.azure.storage.blob.models.BlobCorsRule;
import com.azure.storage.blob.models.BlobServiceProperties;
import com.azure.storage.common.implementation.connectionstring.StorageConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;

@Configuration
public class AzureStorageBlobConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageBlobConfiguration.class);

  private static final ClientLogger CLIENT_LOGGER =
      new ClientLogger(AzureStorageBlobConfiguration.class);

  @Value("${azure.storage.container-name}")
  private String containerName;

  @Value("${azure.storage.connection-string}")
  private String connectionString;

  @Bean
  public BlobServiceClient azureBlobServiceClient(Environment environment)
      throws NoSuchFieldException, IllegalAccessException {
    LOGGER.debug("Initializing access to azure storage blob service ...");

    BlobServiceClient blobServiceClient =
        new BlobServiceClientBuilder().connectionString(connectionString).buildClient();

    // Overwrite blob store URL to use correct host / port / account in docker-compose
    if (environment.acceptsProfiles(Profiles.of("local"))) {
      StorageConnectionString storageConnectionString =
          StorageConnectionString.create(connectionString, CLIENT_LOGGER);

      // Get BlobServiceAsyncClient
      Field blobServiceAsyncClientField =
          BlobServiceClient.class.getDeclaredField("blobServiceAsyncClient");
      blobServiceAsyncClientField.setAccessible(true);
      BlobServiceAsyncClient blobServiceAsyncClient =
          (BlobServiceAsyncClient) blobServiceAsyncClientField.get(blobServiceClient);

      // Get blob storage configuration with URL to overwrite
      Field azureBlobStorageField =
          BlobServiceAsyncClient.class.getDeclaredField("azureBlobStorage");
      azureBlobStorageField.setAccessible(true);
      AzureBlobStorageImpl azureBlobStorage =
          (AzureBlobStorageImpl) azureBlobStorageField.get(blobServiceAsyncClient);

      // Set correct azure blob storage uri
      Field urlField = AzureBlobStorageImpl.class.getDeclaredField("url");
      urlField.setAccessible(true);
      urlField.set(azureBlobStorage, storageConnectionString.getBlobEndpoint().getPrimaryUri());
    }

    LOGGER.info(
        "Blob storage has {} containers",
        blobServiceClient.listBlobContainers().spliterator().getExactSizeIfKnown());

    LOGGER.info(
        "Successfully initialized access to azure storage blob service {}",
        blobServiceClient.getAccountUrl());

    // Enable CORS for local azure storage account emulator
    BlobCorsRule corsRule = new BlobCorsRule();
    corsRule.setAllowedHeaders("*");
    corsRule.setAllowedOrigins("*");
    corsRule.setAllowedMethods("GET, OPTIONS");
    corsRule.setExposedHeaders("*");
    corsRule.setMaxAgeInSeconds(86400);
    BlobServiceProperties properties = new BlobServiceProperties();
    properties.setCors(List.of(corsRule));
    blobServiceClient.setProperties(properties);

    return blobServiceClient;
  }

  @Bean
  public BlobContainerClient azureBlobContainerClient(BlobServiceClient blobServiceClient) {

    BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);

    if (!blobContainerClient.exists()) {
      Response<Void> response =
          blobContainerClient.createWithResponse(null, null, null, Context.NONE);
      if (response.getStatusCode() == HttpStatus.CREATED.value()) {
        LOGGER.info("Container {} created", containerName);
      } else if (response.getStatusCode() >= 400) {
        LOGGER.error(
            "Container {} could not be created. Return code {} received.",
            containerName,
            response.getStatusCode());
      } else {
        LOGGER.warn(
            "Unexpected return code {} received while trying to create container {}.",
            response.getStatusCode(),
            containerName);
      }
    }

    return blobContainerClient;
  }
}
