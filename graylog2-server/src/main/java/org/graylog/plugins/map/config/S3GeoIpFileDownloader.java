package org.graylog.plugins.map.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Paths;

public class S3GeoIpFileDownloader {
    private static final Logger LOG = LoggerFactory.getLogger(S3GeoIpFileDownloader.class);

    private final String s3DownloadLocation;
    private final S3Client s3Client;

    @Inject
    public S3GeoIpFileDownloader(@Named(GeoIpProcessorConfig.S3_DOWNLOAD_LOCATION) String s3DownloadLocation) {

        this.s3DownloadLocation = s3DownloadLocation;
        final S3ClientBuilder clientBuilder = S3Client.builder();
        clientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
        this.s3Client = clientBuilder.build();
    }

    public void downloadFiles(GeoIpResolverConfig config) {

        String asnFile = config.asnDbPath();
        String cityFile = config.cityDbPath();
        try {
            GetObjectResponse response = s3Client.getObject(
                    GetObjectRequest.builder().bucket("zack-testing").key("asn.mmdb").build(),
                    Paths.get(s3DownloadLocation, "downloaded-asn.mmdb"));
            LOG.info("ASN response value: {}", response.toString());

            response = s3Client.getObject(
                    GetObjectRequest.builder().bucket("zack-testing").key("standard_location.mmdb").build(),
                    Paths.get(s3DownloadLocation, "downloaded-standard_location.mmdb"));
            LOG.info("City response value: {}", response.toString());

        } catch (Exception e) {
            LOG.error("Failed to retrieve S3 file. Error: {}", e.toString());
            e.printStackTrace();
        }
    }


}
