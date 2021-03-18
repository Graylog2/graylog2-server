/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Following the <a href="http://semver.org/">Semantic Versioning specification</a>.
 */
public class Version implements Comparable<Version> {
    private static final Logger LOG = LoggerFactory.getLogger(Version.class);

    /**
     * @deprecated Use {@link #getVersion()}
     */
    @Deprecated
    public final int major;
    /**
     * @deprecated Use {@link #getVersion()}
     */
    @Deprecated
    public final int minor;
    /**
     * @deprecated Use {@link #getVersion()}
     */
    @Deprecated
    public final int patch;
    /**
     * @deprecated Use {@link #getVersion()}
     */
    @Deprecated
    public final String additional;
    /**
     * @deprecated Use {@link #getVersion()}
     */
    @Deprecated
    public final String abbrevCommitSha;

    private final com.github.zafarkhaja.semver.Version version;

    /**
     * Reads the current version from the classpath, using version.properties and git.properties.
     */
    public static final Version CURRENT_CLASSPATH = fromClasspathProperties(Version.from(0, 0, 0, "unknown"));

    /**
     * @deprecated Use {@link #Version(com.github.zafarkhaja.semver.Version)} or {@link #from(int, int, int)}
     */
    @Deprecated
    public Version(int major, int minor, int patch) {
        this(buildSemVer(major, minor, patch, null, null));
    }

    /**
     * @deprecated Use {@link #Version(com.github.zafarkhaja.semver.Version)} or {@link #from(int, int, int, String)}
     */
    @Deprecated
    public Version(int major, int minor, int patch, String additional) {
        this(buildSemVer(major, minor, patch, additional, null));
    }

    /**
     * @deprecated Use {@link #Version(com.github.zafarkhaja.semver.Version)}
     */
    @Deprecated
    public Version(int major, int minor, int patch, String additional, String abbrevCommitSha) {
        this(buildSemVer(major, minor, patch, additional, abbrevCommitSha));
    }

    public Version(com.github.zafarkhaja.semver.Version version) {
        this.version = requireNonNull(version);

        // Deprecated
        this.major = version.getMajorVersion();
        this.minor = version.getMinorVersion();
        this.patch = version.getPatchVersion();
        this.additional = version.getPreReleaseVersion();
        this.abbrevCommitSha = version.getBuildMetadata();

    }

    /**
     * Build valid {@link Version} from major, minor, and patch version ("X.Y.Z").
     *
     * @param major The major version component.
     * @param minor The minor version component.
     * @param patch The patch version component.
     * @return The {@link Version} instance built from the given parameters.
     */
    public static Version from(int major, int minor, int patch) {
        return new Version(com.github.zafarkhaja.semver.Version.forIntegers(major, minor, patch));
    }

    /**
     * Build valid {@link Version} from major, minor, and patch version ("X.Y.Z").
     *
     * @param major      The major version component.
     * @param minor      The minor version component.
     * @param patch      The patch version component.
     * @param preRelease The pre-release version component.
     * @return The {@link Version} instance built from the given parameters.
     */
    public static Version from(int major, int minor, int patch, String preRelease) {
        return new Version(buildSemVer(major, minor, patch, preRelease, null));
    }

    /**
     * Build valid {@link Version} from major, minor, and patch version ("X.Y.Z").
     *
     * @param major         The major version component.
     * @param minor         The minor version component.
     * @param patch         The patch version component.
     * @param preRelease    The pre-release version component.
     * @param buildMetadata Additional build metadata (e. g. the Git commit SHA).
     * @return The {@link Version} instance built from the given parameters.
     */
    public static Version from(int major, int minor, int patch, String preRelease, String buildMetadata) {
        return new Version(buildSemVer(major, minor, patch, preRelease, buildMetadata));
    }

    private static com.github.zafarkhaja.semver.Version buildSemVer(int major, int minor, int patch, String preRelease, String buildMetadata) {
        com.github.zafarkhaja.semver.Version version = com.github.zafarkhaja.semver.Version.forIntegers(major, minor, patch);
        if (!isNullOrEmpty(preRelease)) {
            version = version.setPreReleaseVersion(preRelease);
        }
        if (!isNullOrEmpty(buildMetadata)) {
            version = version.setBuildMetadata(buildMetadata);
        }
        return version;
    }

    /**
     * Try to read the version from the {@literal graylog-plugin.properties} file included in a plugin.
     *
     * @param pluginClass     Class where the class loader should be obtained from.
     * @param path            Path of the properties file on the classpath which contains the version information.
     * @param propertyName    The name of the property to read as project version ("major.minor.patch-preReleaseVersion").
     * @param defaultVersion  The {@link Version} to return if reading the information from the properties files failed.
     */
    public static Version fromPluginProperties(Class<?> pluginClass, String path, String propertyName, Version defaultVersion) {
        return fromClasspathProperties(pluginClass, path, propertyName, null, null, defaultVersion);
    }

    /**
     * Try to read the version from the {@literal graylog-plugin.properties} file included in a plugin
     * and {@literal git.properties} ({@code git.commit.id} property) from the classpath..
     *
     * @param pluginClass     Class where the class loader should be obtained from.
     * @param path            Path of the properties file on the classpath which contains the version information.
     * @param propertyName    The name of the property to read as project version ("major.minor.patch-preReleaseVersion").
     * @param gitPath         Path of the properties file on the classpath which contains the SCM information.
     * @param gitPropertyName The name of the property to read as git commit SHA.
     * @param defaultVersion  The {@link Version} to return if reading the information from the properties files failed.
     */
    public static Version fromPluginProperties(Class<?> pluginClass, String path, String propertyName, String gitPath, String gitPropertyName, Version defaultVersion) {
        return fromClasspathProperties(pluginClass, path, propertyName, gitPath, gitPropertyName, defaultVersion);
    }

    /**
     * Try to read the version from {@literal version.properties} ({@code project.version} property)
     * and {@literal git.properties} ({@code git.commit.id} property) from the classpath.
     *
     * @param defaultVersion The {@link Version} to return if reading the information from the properties files failed.
     */
    public static Version fromClasspathProperties(Version defaultVersion) {
        return fromClasspathProperties("version.properties", "project.version", "git.properties", "git.commit.id", defaultVersion);
    }

    /**
     * Try to read the version from {@code path} ({@code project.version} property)
     * and {@code gitPath} ({@code git.commit.id} property) from the classpath.
     *
     * @param path           Path of the properties file on the classpath which contains the version information.
     * @param gitPath        Path of the properties file on the classpath which contains the SCM information.
     * @param defaultVersion The {@link Version} to return if reading the information from the properties files failed.
     */
    public static Version fromClasspathProperties(String path, String gitPath, Version defaultVersion) {
        return fromClasspathProperties(path, "project.version", gitPath, "git.commit.id", defaultVersion);
    }

    /**
     * Try to read the version from {@code path} ({@code propertyName} property)
     * and {@code gitPath} ({@code gitPropertyName} property) from the classpath.
     *
     * @param path            Path of the properties file on the classpath which contains the version information.
     * @param propertyName    The name of the property to read as project version ("major.minor.patch-preReleaseVersion").
     * @param gitPath         Path of the properties file on the classpath which contains the SCM information.
     * @param gitPropertyName The name of the property to read as git commit SHA.
     * @param defaultVersion  The {@link Version} to return if reading the information from the properties files failed.
     */
    public static Version fromClasspathProperties(String path, String propertyName, String gitPath, String gitPropertyName, Version defaultVersion) {
        return fromClasspathProperties(Version.class, path, propertyName, gitPath, gitPropertyName, defaultVersion);
    }

    /**
     * Try to read the version from {@code path} ({@code propertyName} property)
     * and {@code gitPath} ({@code gitPropertyName} property) from the classpath.
     *
     * @param clazz           Class where the class loader should be obtained from.
     * @param path            Path of the properties file on the classpath which contains the version information.
     * @param propertyName    The name of the property to read as project version ("major.minor.patch-preReleaseVersion").
     * @param gitPath         Path of the properties file on the classpath which contains the SCM information.
     * @param gitPropertyName The name of the property to read as git commit SHA.
     * @param defaultVersion  The {@link Version} to return if reading the information from the properties files failed.
     */
    public static Version fromClasspathProperties(@Nonnull Class<?> clazz, String path, String propertyName, String gitPath, String gitPropertyName, Version defaultVersion) {
        try {
            final URL resource = getResource(clazz, path);
            final Properties versionProperties = new Properties();
            versionProperties.load(resource.openStream());

            final com.github.zafarkhaja.semver.Version version = com.github.zafarkhaja.semver.Version.valueOf(versionProperties.getProperty(propertyName));
            final int major = version.getMajorVersion();
            final int minor = version.getMinorVersion();
            final int patch = version.getPatchVersion();
            final String qualifier = version.getPreReleaseVersion();

            String commitSha = null;
            try {
                final Properties git = new Properties();
                final URL gitResource = getResource(clazz, gitPath);
                git.load(gitResource.openStream());
                commitSha = git.getProperty(gitPropertyName);
                // abbreviate if present and looks like a long sha
                if (commitSha != null && commitSha.length() > 7) {
                    commitSha = commitSha.substring(0, 7);
                }
            } catch (Exception e) {
                LOG.debug("Git commit details are not available, skipping.", e);
            }

            return from(major, minor, patch, qualifier, commitSha);
        } catch (Exception e) {
            LOG.error("Unable to read " + path + ", this build has no version number.", e);
        }
        return defaultVersion;
    }

    private static URL getResource(Class<?> clazz, String path) {
        final URL url = requireNonNull(clazz, "Class argument is null!").getClassLoader().getResource(path);

        return requireNonNull(url, "Resource <" + path + "> not found.");
    }

    /**
     * @return the underlying {@link com.github.zafarkhaja.semver.Version} instance.
     */
    public com.github.zafarkhaja.semver.Version getVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return version.toString();
    }

    /**
     * Check if this version is higher than the passed other version. Only taking major and minor version number in account.
     *
     * @param other {@link Version} to compare
     */
    @Deprecated
    public boolean greaterMinor(Version other) {
        return other.major < this.major || other.major == this.major && other.minor < this.minor;
    }

    /**
     * @see com.github.zafarkhaja.semver.Version#greaterThanOrEqualTo(com.github.zafarkhaja.semver.Version)
     */
    public boolean sameOrHigher(Version other) {
        if (isNullOrEmpty(version.getPreReleaseVersion())) {
            return version.greaterThanOrEqualTo(other.getVersion());
        } else {
            // If this is a pre-release version, use the major.minor.patch version for comparison with the other.
            // This allows plugins to require a server version of 2.1.0 and it still gets loaded on a 2.1.0-beta.2 server.
            // See: https://github.com/Graylog2/graylog2-server/issues/2462
            return com.github.zafarkhaja.semver.Version.valueOf(version.getNormalVersion()).greaterThanOrEqualTo(other.getVersion());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Version that = (Version) o;
        return version.equals(that.getVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return version.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@Nonnull Version that) {
        requireNonNull(that);
        return version.compareTo(that.getVersion());
    }
}
