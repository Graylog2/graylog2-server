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
package org.graylog.testing.elasticsearch.multiversion;

import com.google.common.collect.ImmutableSet;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionInvocationContext implements TestTemplateInvocationContext {
    private static final Map<String, ElasticsearchInstance> instanceByVersion = new HashMap<>();
    private final String version;

    public static VersionInvocationContext forVersion(String version) {
        if (!instanceByVersion.containsKey(version)) {
            instanceByVersion.put(version, ElasticsearchInstance.forVersion(version));
        }
        return new VersionInvocationContext(version);
    }

    private VersionInvocationContext(String version) {
        this.version = version;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return version;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return Collections.singletonList(new ParameterResolver() {
            @Override
            public boolean supportsParameter(ParameterContext parameterContext,
                                             ExtensionContext extensionContext) {
                return ImmutableSet.of(Client.class, String.class)
                        .contains(typeFrom(parameterContext));
            }

            private Class<?> typeFrom(ParameterContext parameterContext) {
                return parameterContext.getParameter().getType();
            }

            @Override
            public Object resolveParameter(ParameterContext parameterContext,
                                           ExtensionContext extensionContext) {
                Class<?> type = typeFrom(parameterContext);
                if (type.equals(String.class)) {
                    return version;
                } else if (type.equals(Client.class)) {
                    return instanceByVersion.get(version).client();
                }
                throw new IllegalArgumentException("Unsupported parameter type: " + type);
            }
        });
    }
}
