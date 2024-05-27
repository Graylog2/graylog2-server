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
import type { SeriesUnitJson } from 'views/logic/aggregationbuilder/SeriesUnit';
import SeriesUnit from 'views/logic/aggregationbuilder/SeriesUnit';

import FieldType from './FieldType';
import type { FieldTypeJSON } from './FieldType';

export type FieldTypeMappingJSON = {
  name: string,
  type: FieldTypeJSON,
  unit?: SeriesUnitJson,
};

class FieldTypeMapping {
  value: {
    name: string,
    type: FieldType,
    unit?: SeriesUnit,
  };

  constructor(name: string, type: FieldType, unit: SeriesUnit) {
    this.value = { name, type, unit };
  }

  get name() {
    return this.value.name;
  }

  get type() {
    return this.value.type;
  }

  get unit() {
    return this.value.unit;
  }

  static fromJSON(value: FieldTypeMappingJSON) {
    const { name, type, unit } = value;

    return new FieldTypeMapping(name, FieldType.fromJSON(type), SeriesUnit.fromJSON(unit));
  }

  static create(name: string, type: FieldType, unit: SeriesUnit) {
    return new FieldTypeMapping(name, type, unit);
  }
}

export default FieldTypeMapping;
