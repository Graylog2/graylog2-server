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
package org.graylog.datanode;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.exec.OS;
import org.graylog.datanode.configuration.OpensearchArchitecture;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class OpensearchDistribution {

    private final Path directory;
    private final String version;
    private final String platform;
    private final OpensearchArchitecture architecture;
    private final Supplier<OpensearchDistributionProperties> opensearchDistributionConfig;
    private final Collection<OpensearchDistribution> otherCandidates;

    public OpensearchDistribution(Path directory, String version, @Nullable String platform, @Nullable OpensearchArchitecture architecture) {
        this(directory, version, platform, architecture, Collections.emptyList());
    }

    public OpensearchDistribution(Path path, String version) {
        this(path, version, null, null, Collections.emptyList());
    }

    private OpensearchDistribution(Path directory, String version, String platform, OpensearchArchitecture architecture, List<OpensearchDistribution> otherCandidates) {
        this.directory = directory;
        this.version = version;
        this.platform = platform;
        this.architecture = architecture;
        this.opensearchDistributionConfig = Suppliers.memoize(() -> OpensearchDistributionProperties.forVersion(version));
        this.otherCandidates = otherCandidates;
    }

    public Path getOpensearchBinDirPath() {
        return directory.resolve("bin");
    }

    public Path getOpensearchExecutable() {
        return getOpensearchBinDirPath().resolve("opensearch");
    }

    public Path getOpensearchJavaHome() {
        if (OS.isFamilyMac()) {
            return directory.resolve("jdk-mac");
        } else {
            return directory.resolve("jdk");
        }
    }

    public OpensearchDistributionProperties distributionProperties() {
        return opensearchDistributionConfig.get();
    }

    public Path directory() {
        return directory;
    }

    public String version() {
        return version;
    }

    public String platform() {
        return platform;
    }

    public OpensearchArchitecture architecture() {
        return architecture;
    }

    public Collection<OpensearchDistribution> otherCandidates() {
        return otherCandidates;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        final OpensearchDistribution that = (OpensearchDistribution) o;
        return Objects.equals(directory, that.directory) && Objects.equals(version, that.version) && Objects.equals(platform, that.platform) && architecture == that.architecture;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(directory);
        result = 31 * result + Objects.hashCode(version);
        result = 31 * result + Objects.hashCode(platform);
        result = 31 * result + Objects.hashCode(architecture);
        return result;
    }

    public OpensearchDistribution withOtherCandidates(List<OpensearchDistribution> otherCandidates) {
        return new OpensearchDistribution(directory, version, platform, architecture, otherCandidates);
    }
}
