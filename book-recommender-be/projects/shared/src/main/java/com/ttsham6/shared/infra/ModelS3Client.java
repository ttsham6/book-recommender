package com.ttsham6.shared.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Component
public class ModelS3Client {

  private static final Logger logger = LoggerFactory.getLogger(ModelS3Client.class);
  private final S3Client awsS3Client;

  public ModelS3Client(S3Client awsS3Client) {
    this.awsS3Client = awsS3Client;
  }

  public void downloadModel(String bucket, String prefix, String localDir)
      throws ModelS3ClientException {
    try {
      final var targetDir = Paths.get(localDir);

      // ダウンロード前に対象ディレクトリの中身を空にする（親ディレクトリは残す）
      try {
        if (Files.exists(targetDir)) {
          Files.walk(targetDir)
              .sorted(Comparator.reverseOrder())
              .filter(p -> !p.equals(targetDir))
              .forEach(
                  p -> {
                    try {
                      Files.deleteIfExists(p);
                    } catch (IOException ex) {
                      logger.warn("Failed to delete path {}: {}", p, ex.getMessage());
                    }
                  });
        } else {
          Files.createDirectories(targetDir);
        }
      } catch (IOException e) {
        throw new ModelS3ClientException(
            "Failed to prepare local directory before download: " + localDir, e);
      }

      logger.info("Downloading models from S3 bucket: {}, prefix: {}", bucket, prefix);

      final var listRequest = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();

      final var objects = awsS3Client.listObjectsV2(listRequest).contents();
      if (objects.isEmpty()) {
        logger.warn("No files found in S3 bucket {} with prefix {}", bucket, prefix);
        return;
      }

      for (final var s3Object : objects) {
        final var key = s3Object.key();
        final var fileName = key.substring(prefix.length());
        if (key.endsWith("/") || fileName.isEmpty()) {
          continue;
        }

        final var targetPath = targetDir.resolve(fileName);
        if (targetPath.getParent() != null) {
          Files.createDirectories(targetPath.getParent());
        }

        logger.info("Downloading {} to {}", key, targetPath);
        awsS3Client.getObject(
            b -> b.bucket(bucket).key(key), ResponseTransformer.toFile(targetPath));
      }
    } catch (IOException e) {
      throw new ModelS3ClientException("Failed to download model.", e);
    }
  }
}
