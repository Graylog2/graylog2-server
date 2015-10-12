/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin;

import com.google.common.collect.ComparisonChain;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Following semantic versioning.
 * <p/>
 * http://semver.org/
 */
public class Version implements Comparable<Version> {
    private static final Logger LOG = LoggerFactory.getLogger(Version.class);

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
            final String versionProperties = Resources.toString(resource, UTF_8);
            final Properties version = new Properties();
            version.load(new StringReader(versionProperties));

            final int major = Integer.parseInt(version.getProperty("version.major", "0"));
            final int minor = Integer.parseInt(version.getProperty("version.minor", "0"));
            final int incremental = Integer.parseInt(version.getProperty("version.incremental", "0"));
            final String qualifier = version.getProperty("version.qualifier", "unknown");

            String commitSha = null;
            try {
                final Properties git = new Properties();
                final URL gitResource = Resources.getResource("git.properties");
                final String gitProperties = Resources.toString(gitResource, UTF_8);
                git.load(new StringReader(gitProperties));
                commitSha = git.getProperty("git.commit.id.abbrev");
            } catch (Exception e) {
                LOG.debug("Git commit details are not available, skipping.", e);
            }

            tmpVersion = new Version(major, minor, incremental, qualifier, commitSha);
        } catch (Exception e) {
            tmpVersion = new Version(0, 0, 0, "unknown");
            LOG.error("Unable to read version.properties file, this build has no version number. If you get this message during development, you need to run 'Generate Sources' in IDEA or run 'mvn process-resources'.", e);
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

        if (!isNullOrEmpty(additional)) {
            sb.append("-").append(additional);
        }

        if (!isNullOrEmpty(abbrevCommitSha)) {
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
        return (other.major < this.major) || (other.major == this.major && other.minor < this.minor);
    }

    public boolean sameOrHigher(Version other) {
        return (this.major > other.major) ||
                (this.major == other.major && (this.minor > other.minor || (this.minor == other.minor && this.patch >= other.patch)));
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version that = (Version) o;

        return Objects.equals(this.major, that.major)
                && Objects.equals(this.minor, that.minor)
                && Objects.equals(this.patch, that.patch)
                && Objects.equals(this.additional, that.additional)
                && Objects.equals(this.abbrevCommitSha, that.abbrevCommitSha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, additional, abbrevCommitSha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Version that) {
        checkNotNull(that);
        return ComparisonChain.start()
                .compare(this.major, that.major)
                .compare(this.minor, that.minor)
                .compare(this.patch, that.patch)
                .compareFalseFirst(isNullOrEmpty(this.additional), isNullOrEmpty(that.additional))
                .compare(nullToEmpty(this.additional), nullToEmpty(that.additional))
                .result();
    }
}
