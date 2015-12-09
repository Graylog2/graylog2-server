/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
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
            final String versionPropertiesString = Resources.toString(resource, UTF_8);
            final Properties versionProperties = new Properties();
            versionProperties.load(new StringReader(versionPropertiesString));

            com.github.zafarkhaja.semver.Version version = com.github.zafarkhaja.semver.Version.valueOf(versionProperties.getProperty("project.version", "0.0.0"));

            final int major = version.getMajorVersion();
            final int minor = version.getMinorVersion();
            final int incremental = version.getPatchVersion();
            final String qualifier = version.getPreReleaseVersion();

            String commitSha = null;
            try {
                final Properties git = new Properties();
                final URL gitResource = Resources.getResource("git.properties");
                final String gitProperties = Resources.toString(gitResource, UTF_8);
                git.load(new StringReader(gitProperties));
                commitSha = git.getProperty("git.commit.id");
                // abbreviate if present and looks like a long sha
                if (commitSha != null && commitSha.length() > 7) {
                    commitSha = commitSha.substring(0, 7);
                }
            } catch (Exception e) {
                LOG.debug("Git commit details are not available, skipping.", e);
            }

            tmpVersion = new Version(major, minor, incremental, qualifier, commitSha);
        } catch (Exception e) {
            tmpVersion = new Version(0, 0, 0, "unknown");
            LOG.error("Unable to read version.properties file, this build has no version number.", e);
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
