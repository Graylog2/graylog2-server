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
package org.graylog2.jackson;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoValueSubtypeResolver extends StdSubtypeResolver {
    @Override
    protected void _collectAndResolve(AnnotatedClass annotatedType, NamedType namedType, MapperConfig<?> config, AnnotationIntrospector ai, HashMap<NamedType, NamedType> collectedSubtypes) {
        super._collectAndResolve(annotatedType, resolveAutoValue(namedType), config, ai, collectedSubtypes);
    }

    @Override
    protected void _collectAndResolveByTypeId(AnnotatedClass annotatedType, NamedType namedType, MapperConfig<?> config, Set<Class<?>> typesHandled, Map<String, NamedType> byName) {
        super._collectAndResolveByTypeId(annotatedType, resolveAutoValue(namedType), config, typesHandled, byName);
    }

    private NamedType resolveAutoValue(NamedType namedType) {
        final Class<?> cls = namedType.getType();
        if (cls.getSimpleName().startsWith("AutoValue_")) {
            return new NamedType(cls.getSuperclass(), namedType.getName());
        } else {
            return namedType;
        }
    }
}
