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
package org.graylog2.configuration;

import com.google.inject.internal.Annotations;

import java.lang.annotation.Annotation;
import java.util.Objects;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Objects.requireNonNull;

public class GraylogDataDirImpl implements GraylogDataDir {
    private final String value;

    public GraylogDataDirImpl(String value) {
        this.value = requireNonNull(emptyToNull(value), "@GraylogDataDir value cannot be null");
    }

    public static GraylogDataDirImpl dataDir(String value) {
        return new GraylogDataDirImpl(value);
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return GraylogDataDir.class;
    }

    @Override
    public String toString() {
        return "@" + GraylogDataDir.class.getSimpleName() + "(value=" + Annotations.memberValueString(value) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraylogDataDirImpl that = (GraylogDataDirImpl) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }
}
