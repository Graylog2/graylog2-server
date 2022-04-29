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
package org.graylog2.indexer.fieldtypes.util;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TextFieldTypesSeparator {

    private Set<FieldTypeDTO> textFields;
    private Set<FieldTypeDTO> nonTextFields;

    public void separate(final Collection<FieldTypeDTO> fieldTypeDTOs) {
        textFields = fieldTypeDTOs.stream()
                .filter(fieldTypeDTO -> fieldTypeDTO.physicalType().equals("text"))
                .collect(Collectors.toSet());

        nonTextFields = new HashSet<>(fieldTypeDTOs);
        nonTextFields.removeAll(textFields);
    }

    public Set<FieldTypeDTO> getNonTextFields() {
        return nonTextFields;
    }

    public Set<FieldTypeDTO> getTextFields() {
        return textFields;
    }
}
