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

package org.graylog2.shared.filters;

import com.beust.jcommander.internal.Lists;
import org.graylog2.plugin.filters.MessageFilter;

import java.util.Iterator;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class FilterRegistry {
    private final List<MessageFilter> messageFilters;
    public FilterRegistry() {
        messageFilters = Lists.newArrayList();
    }

    public boolean register(MessageFilter messageFilter) {
        return messageFilters.add(messageFilter);
    }

    public int size() {
        return messageFilters.size();
    }

    public List<MessageFilter> all() {
        return messageFilters;
    }
}
