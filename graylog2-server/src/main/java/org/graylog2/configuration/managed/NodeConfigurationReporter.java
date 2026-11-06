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
package org.graylog2.configuration.managed;

import com.github.joschi.jadconfig.info.ParameterDeclaration;
import com.github.joschi.jadconfig.info.ParameterInfo;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.configuration.ConfigurationInfoService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Reports the configuration values of this node, so that they can be shown without asking the node itself. Nothing is
 * read from a node on demand, which is why the user interface keeps working while a node is down.
 * <p>
 * Only parameters in the {@link ConfigurationCatalog} are reported, and only those backed by a configuration bean:
 * {@link ParameterBacking#RUNTIME} parameters are unknown to {@link ConfigurationInfoService}.
 */
@Singleton
public class NodeConfigurationReporter extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(NodeConfigurationReporter.class);

    private final NodeType nodeType;
    private final NodeId nodeId;
    private final ConfigurationCatalog catalog;
    private final ConfigurationInfoService configurationInfoService;
    private final ManagedConfigurationService configurationService;

    @Inject
    public NodeConfigurationReporter(NodeType nodeType,
                                     NodeId nodeId,
                                     ConfigurationCatalog catalog,
                                     ConfigurationInfoService configurationInfoService,
                                     ManagedConfigurationServices configurationServices) {
        this.nodeType = nodeType;
        this.nodeId = nodeId;
        this.catalog = catalog;
        this.configurationInfoService = configurationInfoService;
        this.configurationService = configurationServices.get(nodeType);
    }

    @Override
    protected void startUp() {
        report();
    }

    @Override
    protected void shutDown() {
        // Reported values are deliberately kept, so that the user interface can show them while this node is down
    }

    /**
     * Writes the values this node resolved into the database. Called on startup, and to be called again whenever this
     * node applies a change without restarting.
     */
    public void report() {
        final Map<String, ReportedValue> reportedValues = new LinkedHashMap<>();
        for (final ConfigurationParameter parameter : catalog.get(nodeType)) {
            if (parameter.backing() != ParameterBacking.CONFIG_FILE) {
                continue;
            }
            configurationInfoService.get(parameter.name())
                    .map(NodeConfigurationReporter::toReportedValue)
                    .ifPresent(reported -> reportedValues.put(parameter.name(), reported));
        }

        if (reportedValues.isEmpty()) {
            LOG.debug("No configuration parameters to report for this node.");
            return;
        }

        configurationService.reportNodeValues(nodeId.getNodeId(), reportedValues);
        LOG.debug("Reported {} configuration parameters for node <{}>.", reportedValues.size(), nodeId.getNodeId());

        adoptClusterValues(reportedValues);
    }

    /**
     * Stores the locally resolved value of cluster wide parameters, unless one is stored already. The first node to
     * start therefore fixes the value for all other nodes of the type.
     * <p>
     * Only explicitly configured values are adopted. Adopting defaults would freeze them: a later Graylog version
     * changing a default would have no effect, because the old default would be stored in the database.
     */
    private void adoptClusterValues(Map<String, ReportedValue> reportedValues) {
        for (final ConfigurationParameter parameter : catalog.get(nodeType, ParameterScope.CLUSTER)) {
            final ReportedValue reported = reportedValues.get(parameter.name());
            if (reported == null || !reported.isExplicit()) {
                continue;
            }
            final String adoptedFrom = f("adopted from node %s at first start", nodeId.getNodeId());
            if (configurationService.adoptClusterValue(parameter.name(), reported.value(), adoptedFrom)) {
                LOG.info("Adopted the local value of cluster wide configuration parameter <{}> for all {} nodes. " +
                                "Changing it in the configuration file has no effect from now on.",
                        parameter.name(), nodeType.value());
            }
        }
    }

    private static ReportedValue toReportedValue(ParameterInfo parameterInfo) {
        return new ReportedValue(rawValue(parameterInfo), ConfigurationSource.of(parameterInfo));
    }

    /**
     * The raw value of a parameter. {@link ParameterInfo#value()} is {@code null} while a parameter sits at the
     * default of its configuration bean, so the default is read from the declarations instead. Both are {@code null}
     * for sensitive parameters, which JadConfig never stores.
     */
    @Nullable
    private static String rawValue(ParameterInfo parameterInfo) {
        if (!parameterInfo.isDefault()) {
            return parameterInfo.value();
        }
        return parameterInfo.declarations().stream()
                .map(ParameterDeclaration::defaultValueAsString)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}
