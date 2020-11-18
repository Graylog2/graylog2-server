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
package org.graylog2.shared.system.stats;

import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SigarService {
    private static final Logger LOG = LoggerFactory.getLogger(SigarService.class);
    private final Sigar sigar;

    @Inject
    public SigarService() {
        Sigar sigar = null;
        try {
            sigar = new Sigar();
            Sigar.load();
            LOG.debug("Successfully loaded SIGAR {}", Sigar.VERSION_STRING);
        } catch (Throwable t) {
            LOG.info("Failed to load SIGAR. Falling back to JMX implementations.");
            LOG.debug("Reason for SIGAR loading failure", t);

            if (sigar != null) {
                try {
                    sigar.close();
                } catch (Throwable t1) {
                    // ignore
                } finally {
                    sigar = null;
                }
            }
        }
        this.sigar = sigar;
    }

    public boolean isReady() {
        return null != sigar;
    }

    public Sigar sigar() {
        return sigar;
    }
}