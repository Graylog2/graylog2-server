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

@AutoValue
public abstract class NetFlowV9ScopeDef {
    public static final int SYSTEM = 1;
    public static final int INTERFACE = 2;
    public static final int LINECARD = 3;
    public static final int NETFLOW_CACHE = 4;
    public static final int TEMPLATE = 5;

    public abstract int type();

    public abstract int length();

    public static NetFlowV9ScopeDef create(int type, int length) {
        return new AutoValue_NetFlowV9ScopeDef(type, length);
    }
}
