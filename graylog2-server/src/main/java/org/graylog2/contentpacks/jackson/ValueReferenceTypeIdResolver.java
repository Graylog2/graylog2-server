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
package org.graylog2.contentpacks.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.graylog2.contentpacks.model.entities.TypedEntity;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ValueReferenceTypeIdResolver extends TypeIdResolverBase {
    private final Map<String, JavaType> subtypes;

    protected ValueReferenceTypeIdResolver(JavaType baseType, TypeFactory typeFactory, Collection<NamedType> subtypes) {
        super(baseType, typeFactory);
        this.subtypes = subtypes.stream().collect(Collectors.toMap(NamedType::getName, v -> typeFactory.constructSimpleType(v.getType(), new JavaType[0])));

    }

    @Override
    public String idFromValue(Object value) {
        if (value instanceof TypedEntity) {
            final TypedEntity typedEntity = (TypedEntity) value;
            return typedEntity.typeString();
        } else {
            return null;
        }
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return null;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return subtypes.get(id);
    }
}
