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
package org.graylog2.periodical;

import org.graylog2.Core;
import org.graylog2.plugin.buffers.BufferWatermark;
import org.joda.time.DateTime;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StatisticsPrinterThread implements Runnable {
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 5;
    
    private final Core server;
        
    public StatisticsPrinterThread(Core server) {
        this.server = server;
    }
    
    @Override
    public void run() {
        caches();
        bufferWatermarks();
        memory();
        messageCounts();
    }
    
    private void print(String subtype, String what) {
        DateTime now = new DateTime();
        
        System.out.println("[util][" + subtype + "][" + now + "] " + what);
    }
    
    private void caches() {
        print("caches", "InputCache size: " + server.getInputCache().size());
        print("caches", "OutputCache size: " + server.getOutputCache().size());
    }
    
    private void bufferWatermarks() {
        int ringSize = server.getConfiguration().getRingSize();
        final BufferWatermark oWm = new BufferWatermark(ringSize, server.outputBufferWatermark());
        final BufferWatermark pWm = new BufferWatermark(ringSize, server.processBufferWatermark());
        print("buffers", "OutputBuffer is at " + oWm.getUtilizationPercentage() + "%. [" + oWm.getUtilization() + "/" + ringSize +"]");
        print("buffers", "ProcessBuffer is at " + pWm.getUtilizationPercentage() + "%. [" + pWm.getUtilization() + "/" + ringSize +"]");
    }
    
    private void memory() {
        int mb = 1024*1024;
        Runtime runtime = Runtime.getRuntime();
        
        print("heap", "Used memory (MB): " + (runtime.totalMemory() - runtime.freeMemory()) / mb);
        print("heap", "Free memory (MB): " + runtime.freeMemory() / mb);
        print("heap", "Total memory (MB): " + runtime.totalMemory() / mb);
        print("heap", "Max memory (MB): " + runtime.maxMemory() / mb);
    }
    
    private void messageCounts() {
        print("written", "Messages written to all outputs: " + server.getBenchmarkCounter().get());
        server.getBenchmarkCounter().set(0);
    }
    
}
