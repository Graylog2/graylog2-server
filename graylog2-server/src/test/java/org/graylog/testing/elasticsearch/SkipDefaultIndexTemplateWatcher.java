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
package org.graylog.testing.elasticsearch;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Checks if a test method is using the {@link SkipDefaultIndexTemplate} annotation and exposes that information
 * with the {@link #shouldSkip()} method.
 */
public class SkipDefaultIndexTemplateWatcher extends TestWatcher {
    private boolean skipIndexTemplateCreation = false;

    @Override
    protected void starting(Description description) {
        final SkipDefaultIndexTemplate skip = description.getAnnotation(SkipDefaultIndexTemplate.class);
        this.skipIndexTemplateCreation = skip != null;
    }

    /**
     * Returns true when the currently executed test method has the {@link SkipDefaultIndexTemplate} annotation.
     *
     * @return true when the current test method has the annotation, false otherwise
     */
    public boolean shouldSkip() {
        return skipIndexTemplateCreation;
    }
}
