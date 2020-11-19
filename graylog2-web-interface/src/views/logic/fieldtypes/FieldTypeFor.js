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
// @flow strict
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import inferTypeForSeries from './InferTypeForSeries';
import FieldType from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';

const fieldTypeFor = (field: string, types: (FieldTypeMappingsList | Array<FieldTypeMapping>)): FieldType => {
  if (isFunction(field)) {
    const { type } = inferTypeForSeries(Series.forFunction(field), types);

    return type;
  }

  const fieldType = types && types.find((f) => f.name === field);

  return fieldType ? fieldType.type : FieldType.Unknown;
};

export default fieldTypeFor;
