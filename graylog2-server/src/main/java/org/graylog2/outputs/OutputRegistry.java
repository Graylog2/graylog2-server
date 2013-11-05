/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.outputs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.graylog2.Core;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class OutputRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OutputRegistry.class);

    private final Core core;
    private List<MessageOutput> outputs;

    public OutputRegistry(Core core) {
        this.core = core;

        outputs = Lists.newArrayList();
    }

    public void register(MessageOutput output) {
        this.outputs.add(output);
    }

    public void initialize() {
        for(MessageOutput o : outputs) {
            try {
                o.initialize(new HashMap<String, String>());
                LOG.info("Initialized output <{}>.", o.getClass().getCanonicalName());
            } catch (MessageOutputConfigurationException e) {
                LOG.error("Could not initialize output <{}>", o.getClass().getCanonicalName(), e);
            }
        }
    }

    public List<MessageOutput> get() {
        return new ImmutableList.Builder<MessageOutput>().addAll(outputs).build();
    }

    public int count() {
        return outputs.size();
    }
}
