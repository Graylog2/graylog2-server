/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.buffers.ProcessBuffer;
import org.graylog2.plugin.Message;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GraylogServerStub extends Core {

    public int callsToProcessBufferInserter = 0;
    public Message lastInsertedToProcessBuffer = null;
    private Configuration configurationStub;
    private MetricRegistry fakeMetricRegistry = new MetricRegistry();

    @Override
    public ProcessBuffer getProcessBuffer() {
        // The process buffer stuf writes back to our counter instance variables.
        return new ProcessBufferStub(this);
    }

    public void setConfigurationStub(Configuration stub) {
        this.configurationStub = stub;
    }

    @Override
    public Configuration getConfiguration() {
        if (this.configurationStub == null) {
            return new Configuration();
        } else {
            return this.configurationStub;
        }
    }

    @Override
    public MetricRegistry metrics() {
        return fakeMetricRegistry;
    }

}