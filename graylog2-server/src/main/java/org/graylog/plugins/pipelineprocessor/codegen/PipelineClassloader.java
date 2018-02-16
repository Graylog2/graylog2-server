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
package org.graylog.plugins.pipelineprocessor.codegen;

import java.util.concurrent.atomic.AtomicLong;

public class PipelineClassloader extends ClassLoader {

    public static AtomicLong loadedClasses = new AtomicLong();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        loadedClasses.incrementAndGet();
        return super.loadClass(name);
    }

    public void defineClass(String className, byte[] bytes) {
        super.defineClass(className, bytes, 0, bytes.length);
    }
}
