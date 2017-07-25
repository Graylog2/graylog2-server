/*
 * Copyright 2013 Eediom Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
