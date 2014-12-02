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
package org.graylog2.periodical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.mongodb.MongoException;
import org.graylog2.bundles.BundleService;
import org.graylog2.bundles.ConfigurationBundle;
import org.graylog2.plugin.periodical.Periodical;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

public class ContentPackLoaderPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackLoaderPeriodical.class);
    private static final String RESOURCE_PREFIX = "contentpacks";
    private static final Pattern RESOURCE_PATTERN = Pattern.compile(".*\\.json");

    private final ObjectMapper objectMapper;
    private final BundleService bundleService;

    @Inject
    public ContentPackLoaderPeriodical(ObjectMapper objectMapper, BundleService bundleService) {
        this.objectMapper = objectMapper;
        this.bundleService = bundleService;
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Set<String> resources = new Reflections(RESOURCE_PREFIX, new ResourcesScanner()).getResources(RESOURCE_PATTERN);

        for (String resource : resources) {
            final URL resourceUrl = Resources.getResource(resource);
            final byte[] resourceBytes;
            try {
                resourceBytes = Resources.toByteArray(resourceUrl);
            } catch (IOException e) {
                LOG.warn("Couldn't read " + resource + ". Skipping.", e);
                continue;
            }

            final ConfigurationBundle contentPack;
            try {
                contentPack = objectMapper.readValue(resourceBytes, ConfigurationBundle.class);
            } catch (IOException e) {
                LOG.warn("Couldn't parse content pack " + resource + ". Skipping", e);
                continue;
            }

            if (bundleService.findByNameAndCategory(contentPack.getName(), contentPack.getCategory()) != null) {
                LOG.debug("Content pack {}/{} already exists in database. Skipping.", contentPack.getCategory(), contentPack.getName());
            } else {
                try {
                    final ConfigurationBundle insertedContentPack = bundleService.insert(contentPack);
                    LOG.debug("Successfully inserted content pack {} into database with ID {}", resource, insertedContentPack.getId());
                } catch (MongoException e) {
                    LOG.error("Error while inserting content pack " + resource + " into database. Skipping.", e);
                    continue;
                }
            }
        }
    }
}
