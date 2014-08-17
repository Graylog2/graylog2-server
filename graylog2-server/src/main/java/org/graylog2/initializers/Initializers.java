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
package org.graylog2.initializers;

import com.google.common.collect.Lists;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Initializers {

    private static final Logger LOG = LoggerFactory.getLogger(Initializers.class);
    private final ServerStatus serverStatus;

    private List<Initializer> initializers;

    @Inject
    public Initializers(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
        initializers = Lists.newArrayList();
    }

    public void register(Initializer initializer) {
        if (initializer.masterOnly() && !serverStatus.hasCapability(ServerStatus.Capability.MASTER)) {
            LOG.info("Not registering initializer {} because it is marked as master only.", initializer.getClass().getSimpleName());
            return;
        }

        this.initializers.add(initializer);
    }

    public void initialize() {
        LOG.info("Running [{}] initializers ...", initializers.size());
        for(Initializer i : initializers) {
            try {
                i.initialize(new HashMap<String, String>());
                LOG.info("Initialized initializer <{}>.", i.getClass().getCanonicalName());
            } catch (InitializerConfigurationException e) {
                LOG.error("Could not initialize initializer <" + i.getClass().getCanonicalName() + ">", e);
            }
        }
    }

    public int count() {
        return initializers.size();
    }
}
