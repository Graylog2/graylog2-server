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

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class NetFlowV9Packet {
    public abstract NetFlowV9Header header();

    public abstract ImmutableList<NetFlowV9Template> templates();

    @Nullable
    public abstract NetFlowV9OptionTemplate optionTemplate();

    public abstract ImmutableList<NetFlowV9BaseRecord> records();

    public abstract long dataLength();

    public static NetFlowV9Packet create(NetFlowV9Header header,
                                         List<NetFlowV9Template> templates,
                                         @Nullable NetFlowV9OptionTemplate optionTemplate,
                                         List<NetFlowV9BaseRecord> records,
                                         long dataLength) {
        return new AutoValue_NetFlowV9Packet(header, ImmutableList.copyOf(templates), optionTemplate, ImmutableList.copyOf(records), dataLength);
    }

    @Override
    public String toString() {
        return header().toString();
    }
}
