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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class NetFlowV9Template {
    @JsonProperty("template_id")
    public abstract int templateId();

    @JsonProperty("field_count")
    public abstract int fieldCount();

    @JsonProperty("definitions")
    public abstract ImmutableList<NetFlowV9FieldDef> definitions();

    @JsonCreator
    public static NetFlowV9Template create(@JsonProperty("template_id") int templateId,
                                           @JsonProperty("field_count") int fieldCount,
                                           @JsonProperty("definitions") List<NetFlowV9FieldDef> definitions) {
        return new AutoValue_NetFlowV9Template(templateId, fieldCount, ImmutableList.copyOf(definitions));
    }

}
