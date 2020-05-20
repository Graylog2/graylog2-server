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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.graylog.testing.junit5utils.Annotations.annotationFrom;

public class VersionsContextProvider implements TestTemplateInvocationContextProvider {
    private static final String[] allVersions = {"5.6.12", "6.8.4"};

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {

        ElasticsearchVersions annotation = annotationFrom(context, ElasticsearchVersions.class);

        String[] versions = annotation.versions().length > 0 ? annotation.versions() : allVersions;

        return Arrays.stream(versions).map(VersionInvocationContext::forVersion);
    }
}
