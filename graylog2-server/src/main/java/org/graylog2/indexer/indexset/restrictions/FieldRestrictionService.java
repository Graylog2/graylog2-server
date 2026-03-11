/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.indexset.restrictions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.graylog2.shared.security.RestPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.graylog2.indexer.indexset.fields.FieldRestrictionsField.FIELD_RESTRICTIONS;
import static org.graylog2.shared.utilities.StringUtils.f;

public class FieldRestrictionService {

    private static final String FIELD_RESTRICTIONS_PATH = "$." + FIELD_RESTRICTIONS;
    private final ParseContext parseContext = JsonPath.using(Configuration.defaultConfiguration()
            .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL));

    private final ObjectMapper objectMapper;

    @Inject
    public FieldRestrictionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void checkRestrictions(Map<String, Set<IndexSetFieldRestriction>> indexSetFieldRestrictions,
                                  Object obj1,
                                  Object obj2) {

        DocumentContext doc1 = doc(obj1);
        DocumentContext doc2 = doc(obj2);

        if (indexSetFieldRestrictions != null && !indexSetFieldRestrictions.isEmpty()) {
            if (!Objects.equals(doc1.read(FIELD_RESTRICTIONS_PATH), doc2.read(FIELD_RESTRICTIONS_PATH))) {
                throw new ForbiddenException("Missing permission %s to change field %s!".formatted(
                        RestPermissions.INDEXSETS_FIELD_RESTRICTIONS_EDIT, FIELD_RESTRICTIONS));
            }
            List<String> invalidFields = new ArrayList<>();

            for (Map.Entry<String, Set<IndexSetFieldRestriction>> entry : indexSetFieldRestrictions.entrySet()) {
                Set<IndexSetFieldRestriction> restrictions = entry.getValue();
                restrictions.forEach(r -> {
                    if (r instanceof FieldRestrictionValidator validator && !validator.validate(entry.getKey(), doc1, doc2)) {
                        invalidFields.add(f("%s : %s", entry.getKey(), r.type()));
                    }
                });
            }

            if (!invalidFields.isEmpty()) {
                throw new BadRequestException("The following fields %s violated restrictions!".formatted(invalidFields));
            }
        }
    }

    private DocumentContext doc(Object o) {
        try {
            return parseContext.parse(objectMapper.writeValueAsString(o));
        } catch (JsonProcessingException e) {
            throw new FieldRestrictionException(e);
        }
    }

    public static class FieldRestrictionException extends RuntimeException {
        public FieldRestrictionException(Exception message) {
            super(message);
        }
    }
}
