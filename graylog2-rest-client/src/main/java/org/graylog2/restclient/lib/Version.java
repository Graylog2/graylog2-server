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
package org.graylog2.restclient.lib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class Version implements Comparable<Version> {
    private static final Logger LOG = LoggerFactory.getLogger(Version.class);

    public static final Version VERSION;

    private static final Pattern versionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-(\\S+))?( \\((\\S+)\\))?");

    static {
        Version tmpVersion;
        try {
            final URL resource = Resources.getResource("org/graylog2/restclient/version.properties");
            final String versionProperties = Resources.toString(resource, StandardCharsets.UTF_8);
            final Properties version = new Properties();
            version.load(new StringReader(versionProperties));

            final int major = Integer.parseInt(version.getProperty("version.major", "0"));
            final int minor = Integer.parseInt(version.getProperty("version.minor", "0"));
            final int incremental = Integer.parseInt(version.getProperty("version.incremental", "0"));
            final String qualifier = version.getProperty("version.qualifier", "unknown");

            String commitSha = null;
            try {
                final Properties git = new Properties();
                final URL gitResource = Resources.getResource("org/graylog2/restclient/git.properties");
                final String gitProperties = Resources.toString(gitResource, StandardCharsets.UTF_8);
                git.load(new StringReader(gitProperties));
                commitSha = git.getProperty("git.commit.id.abbrev");
            } catch (Exception e) {
                LOG.debug("Git commit details are not available, skipping.", e);
            }

            tmpVersion = new Version(major, minor, incremental, qualifier, commitSha);
        } catch (Exception e) {
            tmpVersion = new Version(0, 0, 0, "unknown");
            LOG.debug("Unable to read version.properties file", e);
        }
        VERSION = tmpVersion;
    }

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

    @JsonCreator
    public static Version fromString(String version) {
        if (isNullOrEmpty(version))
            return null;

        final Matcher matcher = versionPattern.matcher(version);

        if (matcher.matches()) {
            final int major = Integer.parseInt(matcher.group(1));
            final int minor = Integer.parseInt(matcher.group(2));
            final int patch = Integer.parseInt(matcher.group(3));
            final String additional = matcher.group(5);
            final String sha1 = matcher.group(7);

            if (isNullOrEmpty(sha1)) {
                if (isNullOrEmpty(additional))
                    return new Version(major, minor, patch);
                else
                    return new Version(major, minor, patch, additional);
            } else
                return new Version(major, minor, patch, additional, sha1);
        } else
            throw new IllegalArgumentException("Unable to parse Version string " + version);
    }

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, "", "");
    }

    public Version(int major, int minor, int patch, String additional) {
        this(major, minor, patch, additional, "");
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
                git.load(new FileReader(Resources.getResource("org/graylog2/restclient/git.properties").getFile()));
                commitSha = git.getProperty("git.commit.id.abbrev");
            } catch (Exception e) {
                LOG.info("Git commit details are not available, skipping the current sha", e);
                commitSha = null;
            }
        }
        commitSha1 = commitSha;
    }


    public String getBranchName() {
        return major + "." + minor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(major).append(".").append(minor).append(".").append(patch);

        if (!isNullOrEmpty(additional)) {
            sb.append("-").append(additional);
        }

        if (!isNullOrEmpty(commitSha1)) {
            sb.append(" (").append(commitSha1).append(')');
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version that = (Version) o;

        return Objects.equals(this.major, that.major)
                && Objects.equals(this.minor, that.minor)
                && Objects.equals(this.patch, that.patch)
                && Objects.equals(this.additional, that.additional)
                && Objects.equals(this.commitSha1, that.commitSha1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, additional, commitSha1);
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
