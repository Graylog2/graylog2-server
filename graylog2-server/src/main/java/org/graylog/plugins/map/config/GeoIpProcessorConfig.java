package org.graylog.plugins.map.config;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;

import javax.inject.Singleton;

@Singleton
public class GeoIpProcessorConfig {
    private static final String PREFIX = "geo_ip_processor";
    public static final String S3_DOWNLOAD_LOCATION = PREFIX + "_s3_download_location";

    @Parameter(value = S3_DOWNLOAD_LOCATION, required = true, validator = StringNotBlankValidator.class)
    private String s3DownloadLocation = "/etc/graylog/server/";

}
