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
package org.graylog2.utilities;

import com.google.common.util.concurrent.Service;

import java.util.concurrent.CountDownLatch;

/**
 * Counts down the given latch when the service has finished "starting", i.e. either it runs fine or failed during startup.
 *
 */
public class LatchUpdaterListener extends Service.Listener {
    private final CountDownLatch latch;

    public LatchUpdaterListener(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void running() {
        latch.countDown();
    }

    @Override
    public void failed(Service.State from, Throwable failure) {
        latch.countDown();
    }
}
