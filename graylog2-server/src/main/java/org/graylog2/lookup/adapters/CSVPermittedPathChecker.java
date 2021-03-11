package org.graylog2.lookup.adapters;

import com.google.common.base.Preconditions;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class CSVPermittedPathChecker {

    private final Path permittedDir;

    @Inject
    public CSVPermittedPathChecker(@Named("csv_file_lookup_dir") Path permittedDir) {
        this.permittedDir = permittedDir;
    }

    /**
     * Checks if CSV file is in permitted location is in permitted path.
     *
     * @param csvFilePath the absolute path of the CSV file. No relative paths allowed.
     * @return true if the script is in the permitted location, false if it is not
     */
    boolean checkPath(String csvFilePath) throws IOException {
        Preconditions.checkNotNull(csvFilePath);

        // csvLookupDir is optional. If not provided, then true is a correct response.
        if (permittedDir == null) {
            return true;
        }

        final Path filePath = Paths.get(csvFilePath).toFile().getCanonicalFile().toPath();

        return filePath.startsWith(permittedDir);
    }
}
