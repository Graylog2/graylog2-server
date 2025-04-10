package org.graylog.plugins.map.config;

import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

public class LocalGeoIpFileService extends GeoIpFileService {
    private static final Logger LOG = LoggerFactory.getLogger(LocalGeoIpFileService.class);

    public LocalGeoIpFileService(GeoIpProcessorConfig config) {
        super(config);
    }

    @Override
    public void validateConfiguration(GeoIpResolverConfig config) throws ConfigValidationException {
        //FIXME: Any kind of validation required here?
    }

    @Override
    protected Optional<Instant> downloadCityFile(GeoIpResolverConfig config, Path tempCityPath) throws IOException {
        return Optional.empty();
    }

    @Override
    protected Optional<Instant> downloadAsnFile(GeoIpResolverConfig config, Path tempAsnPath) throws IOException {
        return Optional.empty();
    }

    @Override
    protected boolean isConnected() {
        return false;
    }

    @Override
    protected Optional<Instant> getCityFileServerTimestamp(GeoIpResolverConfig config) {
        return Optional.empty();
    }

    @Override
    protected Optional<Instant> getAsnFileServerTimestamp(GeoIpResolverConfig config) {
        return Optional.empty();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
