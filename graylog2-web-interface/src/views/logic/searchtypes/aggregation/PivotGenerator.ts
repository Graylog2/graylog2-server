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
import isArray from 'lodash/isArray';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import type FieldType from 'views/logic/fieldtypes/FieldType';

export default (fieldName: string | Array<string>, type: FieldType) => {
  const fields = isArray(fieldName) ? fieldName : [fieldName];

  switch (type.type) {
    case 'date':
      return Pivot.create(fields, 'time', { interval: { type: 'auto', scaling: 1.0 } });
    default:
      return Pivot.createValues(fields);
  }
};
