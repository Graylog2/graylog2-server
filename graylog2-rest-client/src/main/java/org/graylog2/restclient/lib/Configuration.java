/**
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
 */
package org.graylog2.restclient.lib;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    // Variables that can be overriden. (for example in tests)
    private static String graylog2ServerUris = Play.application().configuration().getString("graylog2-server.uris");
    private static String userName = Play.application().configuration().getString("local-user.name");
    private static String passwordHash = Play.application().configuration().getString("local-user.password-sha2");
    private static int fieldListLimit = Play.application().configuration().getInt("field_list_limit", 100);

    private static final Integer DEFAULT_TIMEOUT = 5;
    private static LoadingCache<String, Long> timeoutValues;

    static {
        timeoutValues = CacheBuilder.newBuilder().build(new CacheLoader<String, Long>() {
            @Override
            public Long load(String key) throws Exception {
                final Long milliseconds = Play.application().configuration().getMilliseconds("timeout." + key, Long.MIN_VALUE);
                LOG.debug("Loading timeout value into cache from configuration for key {}: {}",
                        key, milliseconds != Long.MIN_VALUE ? milliseconds + " ms" : "Not configured, falling back to default.");
                return milliseconds;
            }
        });
    }

    /**
     * Exposes the config setting application.context for JavaScript.
     * This ensures that the prefix does not end with a '/' or is completely empty so that the javascript code doesn't
     * have to deal with it when assembling the path.
     *
     * @return path prefix for the app
     */
    public static String getApplicationContext() {
        final String prefix = Play.application().configuration().getString("application.context", "");
        if (prefix.isEmpty() || !prefix.endsWith("/")) {
            return prefix;
        }
        String p = prefix;
        while (p.endsWith("/")) {
            p = p.substring(0, p.length());
        }
        return p;
    }

    public static List<String> getServerRestUris() {
        List<String> uris = Lists.newArrayList();

        // TODO make this more robust and fault tolerant. just a quick hack to get it working for now.
        for (String uri : graylog2ServerUris.split(",")) {
            if (uri != null && !uri.endsWith("/")) {
                uri += "/";
            }

            uris.add(uri);
        }

        return uris;
    }

    /**
     * Returns the timeout value in milliseconds to use for the specifed API call.
     *
     * @param name         the name of the API call (configuration key is "timeout." + name
     * @param defaultValue the default value to use if the name wasn't configured explicitely
     * @param timeUnit     the TimeUnit of the default value
     * @return the timeout in milliseconds
     */
    public static long apiTimeout(final String name, Integer defaultValue, TimeUnit timeUnit) {
        try {
            final Long configuredMillis = timeoutValues.get(name);
            if (configuredMillis == Long.MIN_VALUE) {
                return defaultValue == null
                        ? TimeUnit.MILLISECONDS.convert(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        : TimeUnit.MILLISECONDS.convert(defaultValue, timeUnit);
            }
            return configuredMillis;
        } catch (ExecutionException ignored) {
            // we will never reach this
            LOG.error("Unable to read timeout, this should never happen! Returning default timeout of " + DEFAULT_TIMEOUT + " seconds.");
            return DEFAULT_TIMEOUT;
        }
    }

    /**
     * Convenience method to read a potentially configured API timeout value, falls back to the global timeout value.
     *
     * @param name the name of the API call (configuration key is "timeout." + name
     * @return the timeout in milliseconds
     */
    public static long apiTimeout(final String name) {
        return apiTimeout(name, null, null);
    }

    public static int getFieldListLimit() {
        return fieldListLimit;
    }

    public static void setServerRestUris(String URIs) {
        graylog2ServerUris = URIs;
        LOG.info("graylog2-server.uris overridden with <" + URIs + ">.");
    }

    public static void setUserName(String username) {
        LOG.info("local-user.name overridden with <" + username + ">.");
        userName = username;
    }

    public static void setPassword(String password) {
        LOG.info("local-user.password-sha2 overridden with <" + passwordHash + ">.");
        passwordHash = password;
    }

}
