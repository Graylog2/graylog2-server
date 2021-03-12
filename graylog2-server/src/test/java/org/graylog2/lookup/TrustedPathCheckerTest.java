package org.graylog2.lookup;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrustedPathCheckerTest {

    public static final String TRUSTED_PATH = "/trusted-path";
    TrustedPathChecker pathChecker;

    @Test
    public void success() throws IOException {
        final Set<Path> paths = Collections.singleton(Paths.get(TRUSTED_PATH));
        pathChecker = new TrustedPathChecker(paths);
        assertTrue(pathChecker.fileIsInTrustedPath(TRUSTED_PATH + "/file.csv"));
    }

    @Test
    public void failureOutsideOfTrustedPath() throws IOException {
        final Set<Path> paths = Collections.singleton(Paths.get(TRUSTED_PATH));
        pathChecker = new TrustedPathChecker(paths);
        assertFalse(pathChecker.fileIsInTrustedPath("/untrusted-path/file.csv"));
    }
}
