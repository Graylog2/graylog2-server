/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015 Graylog, Inc. (hello@graylog.org)
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
/*
* Modified by Benjamin H. Klimkowski, bhklimk@gmail.com
*/
package org.graylog.plugins.netflow;

import org.graylog.plugins.netflow.codecs.NetFlowCodec;
import org.graylog.plugins.netflow.inputs.NetFlowUdpInput;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class NetFlowPluginModule extends PluginModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        addMessageInput(NetFlowUdpInput.class);
        addCodec("netflow", NetFlowCodec.class);
    }
}
