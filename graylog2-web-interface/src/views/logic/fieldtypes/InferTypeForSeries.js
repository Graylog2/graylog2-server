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
import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import FieldType, { FieldTypes } from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';

const typePreservingFunctions = ['avg', 'min', 'max', 'percentile'];
const constantTypeFunctions = {
  card: FieldTypes.LONG,
  count: FieldTypes.LONG,
};

const inferTypeForSeries = (series: Series, types: (FieldTypeMappingsList | Array<FieldTypeMapping>)): FieldTypeMapping => {
  const definition = parseSeries(series.function);
  const newMapping = (type) => FieldTypeMapping.create(series.function, type);

  if (definition === null) {
    return newMapping(FieldType.Unknown);
  }

  const { type, field } = definition;

  // $FlowFixMe: this check should...
  if (constantTypeFunctions[type]) {
    // $FlowFixMe: ... guard this access!
    return newMapping(constantTypeFunctions[type]());
  }

  if (typePreservingFunctions.includes(type)) {
    const mapping = types && types.find((t) => (t.name === field));

    if (!mapping) {
      return newMapping(FieldType.Unknown);
    }

    return newMapping(mapping.type);
  }

  return newMapping(FieldTypes.FLOAT());
};

export default inferTypeForSeries;
