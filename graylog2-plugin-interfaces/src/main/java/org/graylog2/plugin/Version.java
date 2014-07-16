/*
 * Copyright 2012-2014 TORCH GmbH
 *
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
package org.graylog2.plugin;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.net.URL;
import java.util.Properties;

/**
 * Following semantic versioning.
 *
 * http://semver.org/
 */
public class Version {
    private static final Logger log = LoggerFactory.getLogger(Version.class);

    public final int major;
    public final int minor;
    public final int patch;
    public final String additional;
    public final String abbrevCommitSha;

    /**
     * Reads the current version from the classpath, using version.properties and git.properties.
     */
    public static final Version CURRENT_CLASSPATH;
    static {
        Version tmpVersion;
        try {
            final URL resource = Resources.getResource("version.properties");
            final FileReader versionProperties = new FileReader(resource.getFile());
            final Properties version = new Properties();
            version.load(versionProperties);

            final int major = Integer.parseInt(version.getProperty("version.major", "0"));
            final int minor = Integer.parseInt(version.getProperty("version.minor", "0"));
            final int incremental = Integer.parseInt(version.getProperty("version.incremental", "0"));
            final String qualifier = version.getProperty("version.qualifier", "unknown");

            String commitSha = null;
            try {
                final Properties git = new Properties();
                git.load(new FileReader(Resources.getResource("git.properties").getFile()));
                commitSha = git.getProperty("git.commit.id.abbrev");
            } catch (Exception e) {
                log.debug("Git commit details are not available, skipping.", e);
            }

            tmpVersion = new Version(major, minor, incremental, qualifier, commitSha);
        } catch (Exception e) {
            tmpVersion = new Version(0, 0, 0, "unknown");
            log.error("Unable to read version.properties file, this build has no version number. If you get this message during development, you need to run 'Generate Sources' in IDEA or run 'mvn process-resources'.", e);
        }
        CURRENT_CLASSPATH = tmpVersion;
    }

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, null, null);
    }

    public Version(int major, int minor, int patch, String additional) {
        this(major, minor, patch, additional, null);
    }

    public Version(int major, int minor, int patch, String additional, String abbrevCommitSha) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.additional = additional;
        this.abbrevCommitSha = abbrevCommitSha;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(major).append(".").append(minor).append(".").append(patch);

        if (additional != null && !additional.isEmpty()) {
            sb.append("-").append(additional);
        }

        if (abbrevCommitSha != null) {
            sb.append(" (").append(abbrevCommitSha).append(')');
        }

        return sb.toString();
    }

    /**
     * Check if this version is higher than the passed other version. Only taking major and minor version number in account.
     *
     * @param other version to compare
     * @return
     */
    public boolean greaterMinor(Version other) {
        if (other.major < this.major) {
            return true;
        }

        if (other.major == this.major && other.minor < this.minor) {
            return true;
        }

        return false;
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
