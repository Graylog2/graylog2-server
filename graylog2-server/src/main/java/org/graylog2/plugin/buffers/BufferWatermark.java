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
package org.graylog2.plugin.buffers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BufferWatermark {
    
    private final int bufferSize;
    private final AtomicLong watermark;
    
    public BufferWatermark(int bufferSize, AtomicLong watermark) {
        this.bufferSize = bufferSize;
        this.watermark = watermark;
    }
    
    public long getUtilization() {
        return watermark.get();
    }
    
    public float getUtilizationPercentage() {
        return (float) getUtilization()/bufferSize*100;
    }
    
}
