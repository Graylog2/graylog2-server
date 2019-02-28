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
package org.graylog2.system.shutdown;

/**
 * Services can implement this to participate in a graceful shutdown of the server.
 * <p>
 * A service can register itself with the {@link GracefulShutdownService} like this:
 *
 * <pre> {@code
 * class MyService implements GracefulShutdownHook {
 *     private final GracefulShutdownService shutdownService;
 *
 *     @Inject
 *     public MyService(GracefulShutdownService shutdownService) {
 *         this.shutdownService = shutdownService;
 *     }
 *
 *     // This will be executed by the GracefulShutdownService on server shutdown
 *     @Override
 *     void doGracefulShutdown() throws Exception {
 *         runShutdownTasks();
 *     }
 *
 *     public void start() {
 *         // Let the GracefulShutdownService know about this service
 *         this.shutdownService.register(this);
 *     }
 *
 *     // This will be executed by some service manager when this service is stopped
 *     public void stop() {
 *         // Remove this service from the GracefulShutdownService because it's stopped before server shutdown and
 *         // we don't need any graceful shutdown for it anymore
 *         this.shutdownService.unregister(this);
 *         runShutdownTasks();
 *     }
 *
 *     private void runShutdownTasks() {
 *         // Run the actual shutdown tasks for the service here
 *     }
 * }
 * }</pre>
 */
public interface GracefulShutdownHook {
    /**
     * Execute shutdown tasks for the service that implements this interface.
     * <h3>Warning:</h3>
     * <ul>
     *     <li>This method is called from another thread so the class that implements {@link GracefulShutdownHook} must be thread-safe.</li>
     *     <li>The server shutdown is waiting for this method call to complete. Blocking this method will block the server
     * shutdown!</li>
     * </ul>
     *
     * @throws Exception
     */
    void doGracefulShutdown() throws Exception;
}
