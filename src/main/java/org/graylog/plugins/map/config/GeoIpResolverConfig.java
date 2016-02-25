package org.graylog.plugins.map.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class GeoIpResolverConfig {
    public enum DatabaseType {
        MAXMIND_CITY, MAXMIND_COUNTRY
    }

    @JsonProperty("enabled")
    public abstract boolean enabled();

    @JsonProperty("db_type")
    public abstract DatabaseType dbType();

    @JsonProperty("db_path")
    public abstract String dbPath();

    @JsonProperty("run_before_extractors")
    public abstract boolean runBeforeExtractors();

    @JsonCreator
    public static GeoIpResolverConfig create(@JsonProperty("enabled") boolean enabled,
                                             @JsonProperty("db_type") DatabaseType dbType,
                                             @JsonProperty("db_path") String dbPath,
                                             @JsonProperty("run_before_extractors") boolean runBeforeExtractors) {
        return builder()
                .enabled(enabled)
                .dbType(dbType)
                .dbPath(dbPath)
                .runBeforeExtractors(runBeforeExtractors)
                .build();
    }

    public static GeoIpResolverConfig defaultConfig() {
       return builder()
               .enabled(false)
               .dbType(DatabaseType.MAXMIND_CITY)
               .dbPath("/tmp/GeoLite2-City.mmdb")
               .runBeforeExtractors(false)
               .build();
    }

    public static Builder builder() {
        return new AutoValue_GeoIpResolverConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder enabled(boolean enabled);
        public abstract Builder dbType(DatabaseType dbType);
        public abstract Builder dbPath(String dbPath);
        public abstract Builder runBeforeExtractors(boolean runBeforeExtractors);

        public abstract GeoIpResolverConfig build();
    }
}