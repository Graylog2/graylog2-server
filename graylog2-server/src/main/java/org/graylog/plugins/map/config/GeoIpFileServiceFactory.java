package org.graylog.plugins.map.config;

import jakarta.inject.Inject;

public class GeoIpFileServiceFactory {
    private final GeoIpProcessorConfig processorConfig;
    private GeoIpFileService actualS3FileService;
    private GeoIpFileService actualGcsFileService;

    @Inject
    public GeoIpFileServiceFactory(GeoIpProcessorConfig processorConfig) {
        this.processorConfig = processorConfig;
    }

    /**
     * Creates a new instance of {@link GeoIpFileService} if it doesn't already exist.
     * This method is thread-safe and ensures that only one instance is created.
     *
     * @return a new instance of {@link GeoIpFileService}
     */
    public GeoIpFileService create(GeoIpResolverConfig config) {
        if (config.useS3()) {
            if (actualS3FileService == null) {
                actualS3FileService = new S3GeoIpFileService(processorConfig);
            }
            return actualS3FileService;
        } else if (config.useGcs()) {
            if (actualGcsFileService == null) {
                actualGcsFileService = new GcsGeoIpFileService(processorConfig);
            }
            return actualGcsFileService;
        } else {
            return new LocalGeoIpFileService(processorConfig);
        }
    }
}
