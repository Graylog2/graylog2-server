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
import * as Immutable from 'immutable';

export type Property = 'compound' | 'enumerable' | 'full-text-search' | 'numeric' | 'decorated';

export const Properties: { [key: string]: Property } = {
  Compound: 'compound',
  Enumerable: 'enumerable',
  FullTextSearch: 'full-text-search',
  Numeric: 'numeric',
  Decorated: 'decorated',
};

export type FieldTypeJSON = {
  type: string,
  properties: Array<Property>,
  index_names: Array<string>,
};

export type FieldName = string;
export type FieldValue = any;

class FieldType {
  value: Immutable.Map<string, any>;

  constructor(type: string, properties: Array<Property>, indexNames: Array<string>) {
    this.value = Immutable.Map({ type, properties: Immutable.Set(properties), indexNames: Immutable.Set(indexNames) });
  }

  static Unknown = new FieldType('unknown', [], []);

  static Decorated = new FieldType('decorated field', [Properties.Decorated], []);

  get type(): string {
    return this.value.get('type');
  }

  get properties(): Immutable.Set<string> {
    return this.value.get('properties');
  }

  get indexNames(): Immutable.Set<string> {
    return this.value.get('indexNames');
  }

  isNumeric(): boolean {
    return this.properties.has(Properties.Numeric);
  }

  isCompound(): boolean {
    return this.properties.has(Properties.Compound);
  }

  isDecorated(): boolean {
    return this.properties.has(Properties.Decorated);
  }

  static fromJSON(value: FieldTypeJSON) {
    const { type, properties, index_names } = value;

    return new FieldType(type, properties, index_names);
  }

  static create(type: string, properties: Array<Property> = [], indexNames: Array<string> = []) {
    return new FieldType(type, properties, indexNames);
  }
}

export default FieldType;

const createType = (type, properties: Array<Property> = []) => (indices: Array<string> = []) => FieldType.create(type, properties, indices);

export const FieldTypes = {
  STRING: createType('string', [Properties.Enumerable]),
  STRING_FTS: createType('string', [Properties.FullTextSearch]),
  LONG: createType('long', [Properties.Numeric, Properties.Enumerable]),
  INT: createType('int', [Properties.Numeric, Properties.Enumerable]),
  SHORT: createType('short', [Properties.Numeric, Properties.Enumerable]),
  BYTE: createType('byte', [Properties.Numeric, Properties.Enumerable]),
  DOUBLE: createType('double', [Properties.Numeric, Properties.Enumerable]),
  FLOAT: createType('float', [Properties.Numeric, Properties.Enumerable]),
  DATE: createType('date', [Properties.Enumerable]),
  BOOLEAN: createType('boolean', [Properties.Enumerable]),
  BINARY: createType('binary', []),
  GEO_POINT: createType('geo-point', []),
  IP: createType('ip', [Properties.Enumerable]),
};
