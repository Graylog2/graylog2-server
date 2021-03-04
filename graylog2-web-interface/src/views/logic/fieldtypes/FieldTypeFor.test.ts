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
import fieldTypeFor from './FieldTypeFor';
import FieldType, { FieldTypes } from './FieldType';
import FieldTypeMapping from './FieldTypeMapping';

describe('FieldTypeFor', () => {
  it('returns `FieldType.Unknown` if field type is not found', () => {
    expect(fieldTypeFor('', [])).toEqual(FieldType.Unknown);

    expect(fieldTypeFor('bar', [FieldTypeMapping.create('foo', FieldTypes.LONG())]))
      .toEqual(FieldType.Unknown);
  });

  it('returns `FieldType.Unknown` if field types are `undefined`', () => {
    expect(fieldTypeFor('', undefined)).toEqual(FieldType.Unknown);
  });

  it('returns type of field if present', () => {
    expect(fieldTypeFor('foo', [FieldTypeMapping.create('foo', FieldTypes.LONG())]))
      .toEqual(FieldTypes.LONG());
  });

  it('returns inferred type of function if series is passed', () => {
    expect(fieldTypeFor('avg(foo)', [FieldTypeMapping.create('foo', FieldTypes.LONG())]))
      .toEqual(FieldTypes.LONG());

    expect(fieldTypeFor('card(foo)', [FieldTypeMapping.create('foo', FieldTypes.STRING())]))
      .toEqual(FieldTypes.LONG());

    expect(fieldTypeFor('sum(foo-bar)', [FieldTypeMapping.create('foo-bar', FieldTypes.LONG())]))
      .toEqual(FieldTypes.FLOAT());
  });

  it('returns inferred type (long) for `count()`', () => {
    expect(fieldTypeFor('count()', []))
      .toEqual(FieldTypes.LONG());
  });
});
