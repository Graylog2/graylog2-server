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
package org.graylog.plugins.netflow.v9;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class NetFlowV9OptionTemplate {
    public abstract int templateId();

    public abstract ImmutableList<NetFlowV9ScopeDef> scopeDefs();

    public abstract ImmutableList<NetFlowV9FieldDef> optionDefs();

    public static NetFlowV9OptionTemplate create(int templateId,
                                                 List<NetFlowV9ScopeDef> scopeDefs,
                                                 List<NetFlowV9FieldDef> optionDefs) {
        return new AutoValue_NetFlowV9OptionTemplate(templateId, ImmutableList.copyOf(scopeDefs), ImmutableList.copyOf(optionDefs));
    }
}
