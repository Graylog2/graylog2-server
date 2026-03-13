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
import { useContext, useMemo, useCallback } from 'react';
import { OrderedSet } from 'immutable';

import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { defaultCompare } from 'logic/DefaultCompare';

const useFormattedFields = (favoriteFields: Array<string>) => {
  const { message, messageFields: fieldTypes } = useContext(MessageFavoriteFieldsContext);
  const fieldsMapper = useCallback(
    (field: string) => {
      const { type } = fieldTypes.find(
        (t) => t.name === field,
        undefined,
        FieldTypeMapping.create(field, FieldType.Unknown),
      );

      const value = message?.formatted_fields?.[field];

      return { value, field, type, id: field, title: field };
    },
    [fieldTypes, message?.formatted_fields],
  );

  const formattedFavorites = useMemo(() => {
    const formattedFieldSet = OrderedSet(Object.keys(message.formatted_fields));

    return favoriteFields.filter((field) => formattedFieldSet.has(field)).map(fieldsMapper);
  }, [favoriteFields, fieldsMapper, message.formatted_fields]);
  const formattedRest = useMemo(() => {
    const favoriteFieldsSet = OrderedSet(favoriteFields);

    return Object.keys(message.formatted_fields)
      .filter((field) => !favoriteFieldsSet.has(field))
      .sort((key1, key2) => defaultCompare(key1, key2))
      .map(fieldsMapper);
  }, [favoriteFields, fieldsMapper, message.formatted_fields]);

  return { formattedFavorites, formattedRest };
};

export default useFormattedFields;
