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

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Hostname and IP address matcher implementation similar to what the JDK is using in the proxy server selector
 * to support the {@code http.nonProxyHosts} property.
 * <p>
 * The main difference to the implementation in the JDK is, that this one is using {@code ","} as delimiter.
 * <p>
 * See: <https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html>
 */
public class ProxyHostsPattern {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyHostsPattern.class);
    private static final String DELIMITER = ",";

    private final String noProxyHosts;
    private final Pattern pattern;

    private ProxyHostsPattern(String noProxyHosts, Pattern pattern) {
        this.noProxyHosts = noProxyHosts;
        this.pattern = pattern;
    }

    public String getNoProxyHosts() {
        return noProxyHosts;
    }

    public boolean matches(@Nullable final String hostOrIp) {
        if (pattern == null) {
            LOG.debug("No proxy host pattern defined");
            return false;
        }
        if (isNullOrEmpty(hostOrIp)) {
            LOG.debug("Host or IP address <{}> doesn't match <{}>", hostOrIp, noProxyHosts);
            return false;
        }

        if (pattern.matcher(hostOrIp.toLowerCase(Locale.ROOT)).matches()) {
            LOG.debug("Host or IP address <{}> matches <{}>", hostOrIp, noProxyHosts);
            return true;
        } else {
            LOG.debug("Host or IP address <{}> doesn't match <{}>", hostOrIp, noProxyHosts);
            return false;
        }
    }

    public static ProxyHostsPattern create(final String noProxyHosts) {
        if (isNullOrEmpty(noProxyHosts)) {
            return new ProxyHostsPattern("", null);
        }

        final Set<String> patterns = Splitter.on(DELIMITER)
                .trimResults()
                .omitEmptyStrings()
                .splitToList(noProxyHosts)
                .stream()
                .map(ProxyHostsPattern::toPattern)
                .collect(Collectors.toSet());

        if (patterns.isEmpty()) {
            return new ProxyHostsPattern(noProxyHosts, null);
        }
        return new ProxyHostsPattern(noProxyHosts, Pattern.compile(String.join("|", patterns)));
    }

    private static String toPattern(String hostPattern) {
        if (hostPattern.startsWith("*")) {
            return ".*" + Pattern.quote(hostPattern.substring(1).toLowerCase(Locale.ROOT));
        } else if (hostPattern.endsWith("*")) {
            return Pattern.quote(hostPattern.substring(0, hostPattern.length() - 1).toLowerCase(Locale.ROOT)) + ".*";
        } else {
            return Pattern.quote(hostPattern);
        }
    }
}
