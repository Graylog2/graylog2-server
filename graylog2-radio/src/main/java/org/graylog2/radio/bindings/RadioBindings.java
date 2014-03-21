/*
 * Copyright 2013-2014 TORCH GmbH
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.radio.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.shared.ServerStatus;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioBindings extends AbstractModule {
    private final Configuration configuration;

    public RadioBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(configuration);
        install(new FactoryModuleBuilder().build(RadioProcessBufferProcessor.Factory.class));
        bind(ServerStatus.class).toInstance(new ServerStatus(configuration));
    }
}
