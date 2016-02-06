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