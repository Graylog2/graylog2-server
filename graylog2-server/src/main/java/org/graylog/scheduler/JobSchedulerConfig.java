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
package org.graylog.scheduler;

/**
 * Used by the scheduler to configure itself.
 */
public interface JobSchedulerConfig {
    /**
     * Determines if the scheduler can start.
     *
     * @return true if the scheduler can be started, false otherwise
     */
    boolean canStart();

    /**
     * Determines if the scheduler can execute the next loop iteration.
     * This method will be called at the beginning of each scheduler loop iteration so it should be fast!
     *
     * @return true if scheduler can execute next loop iteration
     */
    boolean canExecute();

    /**
     * The number of worker threads to start.
     *
     * @return number of worker threads
     */
    int numberOfWorkerThreads();
}
