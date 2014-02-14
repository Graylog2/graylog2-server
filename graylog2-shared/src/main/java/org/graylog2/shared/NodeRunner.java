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

package org.graylog2.shared;

import com.beust.jcommander.internal.Lists;
import com.google.inject.Module;
import org.graylog2.shared.bindings.GenericBindings;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NodeRunner {
    protected static List<Module> getBindingsModules(Module specificModule) {
        List<Module> result = Lists.newArrayList();
        result.add(new GenericBindings());
        result.add(specificModule);
        return result;
    }
}
