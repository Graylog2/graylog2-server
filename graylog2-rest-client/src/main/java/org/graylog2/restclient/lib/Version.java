/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.lib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.Properties;

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
    public static final Version v0_21_0_BETA4 = new Version(0, 21, 0, "beta4");
    public static final Version v0_21_0_RC_1 = new Version(0, 21, 0, "rc.1");

    public static final Version v0_90_0 = new Version(0, 90, 0);
    public static final Version v0_90_1 = new Version(0, 90, 1);
    public static final Version v0_90_2 = new Version(0, 90, 2);
    public static final Version v0_91_0 = new Version(0, 91, 0);
    public static final Version v0_91_1 = new Version(0, 91, 1);
    public static final Version v0_91_2 = new Version(0, 91, 2);

    public static final Version v0_92_0_SNAPSHOT = new Version(0, 92, 0, "snapshot");
    public static final Version v0_92_0_BETA_1 = new Version(0, 92, 0, "beta.1");
    public static final Version v0_92_0_RC_1 = new Version(0, 92, 0, "rc.1");
    public static final Version v0_92_0 = new Version(0, 92, 0);

    public static final Version VERSION = v0_92_0;

    @JsonProperty
    public int major;

    @JsonProperty
    public int minor;

    @JsonProperty
    public int patch;

    @JsonProperty
    public String additional;

    @JsonProperty
    private String commitSha1;

    @JsonCreator
    public Version() {
        major = 0;
        minor = 0;
        patch = 0;
        additional = null;
        commitSha1 = null;
    }

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
