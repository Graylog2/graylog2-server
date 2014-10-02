/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.lib;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.Properties;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Version {
    private static final Logger log = LoggerFactory.getLogger(Version.class);

    /*
     * Following semantic versioning.
     *
     * http://semver.org/
     */

    public static final Version vDEV = new Version(0, 20, 0, "dev");
    public static final Version v0_20_0_PREVIEW_1 = new Version(0, 20, 0, "preview.1");
    public static final Version v0_20_0_PREVIEW_2 = new Version(0, 20, 0, "preview.2");
    public static final Version v0_20_0_PREVIEW_3 = new Version(0, 20, 0, "preview.3");
    public static final Version v0_20_0_PREVIEW_4 = new Version(0, 20, 0, "preview.4");
    public static final Version v0_20_0_PREVIEW_5 = new Version(0, 20, 0, "preview.5");
    public static final Version v0_20_0_PREVIEW_6 = new Version(0, 20, 0, "preview.6");
    public static final Version v0_20_0_PREVIEW_7 = new Version(0, 20, 0, "preview.7");
    public static final Version v0_20_0_PREVIEW_8 = new Version(0, 20, 0, "preview.8");
    public static final Version v0_20_0_RC_1 = new Version(0, 20, 0, "rc.1");
    public static final Version v0_20_0_RC_1_1 = new Version(0, 20, 0, "rc.1-1");
    public static final Version v0_20_0_RC_2 = new Version(0, 20, 0, "rc.2");
    public static final Version v0_20_0_RC_3 = new Version(0, 20, 0, "rc.3");
    public static final Version v0_20_0 = new Version(0, 20, 0);
    public static final Version v0_20_1 = new Version(0, 20, 1);

    public static final Version v0_20_2_SNAPSHOT = new Version(0, 20, 2, "snapshot");

    public static final Version v0_21_0_SNAPSHOT = new Version(0, 21, 0, "snapshot");
    public static final Version v0_21_0_BETA1 = new Version(0, 21, 0, "beta1");
    public static final Version v0_21_0_BETA2 = new Version(0, 21, 0, "beta2");
    public static final Version v0_21_0_BETA3 = new Version(0, 21, 0, "beta3");
    public static final Version v0_21_0_BETA4_SNAPSHOT = new Version(0, 21, 0, "beta4-snapshot");
    public static final Version v0_21_0_BETA4= new Version(0, 21, 0, "beta4");
    public static final Version v0_21_0_BETA5_SNAPSHOT = new Version(0, 21, 0, "beta5-snapshot");
    public static final Version v0_21_0_RC_1= new Version(0, 21, 0, "rc.1");
    public static final Version v0_23_0_SNAPSHOT = new Version(0, 23, 0, "snapshot");

    public static final Version VERSION = v0_23_0_SNAPSHOT;

    public final int major;
    public final int minor;
    public final int patch;
    public final String additional;
    private final String commitSha1;

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, null, null);
    }
    public Version(int major, int minor, int patch, String additional) {
        this(major, minor, patch, additional, null);
    }

    public Version(int major, int minor, int patch, String additional, String sha1) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.additional = additional;

        String commitSha = sha1;
        if (sha1 == null) {
            // try to read it from git.properties
            try {
                final Properties git = new Properties();
                git.load(new FileReader(Resources.getResource("git.properties").getFile()));
                commitSha = git.getProperty("git.sha1");
                commitSha = commitSha.substring(0, 7); // 7 chars is enough usually
            } catch (Exception e) {
                log.info("Git commit details are not available, skipping the current sha", e);
                commitSha = null;
            }
        }
        commitSha1 = commitSha;
    }


    public String getBranchName() {
        return major + "." + minor;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(major).append(".").append(minor).append(".").append(patch);

        if (additional != null && !additional.isEmpty()) {
            sb.append("-").append(additional);
        }

        if (commitSha1 != null) {
            sb.append(" (").append(commitSha1).append(")");
        }

        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        Version version = (Version) obj;

        return toString().equals(version.toString());
    }

}
