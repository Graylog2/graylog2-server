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
import FieldType from './FieldType';
import type { FieldTypeJSON } from './FieldType';

export type FieldTypeMappingJSON = {
  name: string,
  type: FieldTypeJSON,
};

class FieldTypeMapping {
  value: {
    name: string,
    type: FieldType,
  };

  constructor(name: string, type: FieldType) {
    this.value = { name, type };
  }

  get name() {
    return this.value.name;
  }

  get type() {
    return this.value.type;
  }

  static fromJSON(value: FieldTypeMappingJSON) {
    const { name, type } = value;

    return new FieldTypeMapping(name, FieldType.fromJSON(type));
  }

  static create(name: string, type: FieldType) {
    return new FieldTypeMapping(name, type);
  }
}

export default FieldTypeMapping;
