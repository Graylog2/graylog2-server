package org.graylog.datanode.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.PathReadableValidator;
import org.graylog2.configuration.Documentation;

import java.nio.file.Path;

public class GCSRepositoryConfiguration {
    @Documentation("Path to Google Cloud Storage credentials file")
    @Parameter(value = "gcs_credentials_file", validators = PathReadableValidator.class)
    private Path gcsCredentialsFile;

    public Path getGcsCredentialsFile() {
        return gcsCredentialsFile;
    }

    public boolean isRepositoryEnabled() {
        return gcsCredentialsFile != null;
    }
}
