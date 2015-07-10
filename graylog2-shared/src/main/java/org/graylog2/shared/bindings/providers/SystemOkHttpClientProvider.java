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
package org.graylog2.shared.bindings.providers;

import com.github.joschi.jadconfig.util.Duration;

import javax.inject.Singleton;

/**
 * Provider for a configured {@link com.squareup.okhttp.OkHttpClient} used for system-level tasks.
 */
@Singleton
public class SystemOkHttpClientProvider extends OkHttpClientProvider {
    public SystemOkHttpClientProvider() {
        super(
                Duration.seconds(2L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                null);
    }
}
