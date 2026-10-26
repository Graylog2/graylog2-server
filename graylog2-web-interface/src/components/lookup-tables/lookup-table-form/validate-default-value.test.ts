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
import validateDefaultValue from './validate-default-value';

describe('validateDefaultValue', () => {
  it.each([
    ['anything', 'STRING'],
    ['', 'STRING'],
    ['', 'NULL'],
    ['hello', 'NULL'],
    ['42', 'NUMBER'],
    ['-1.5e3', 'NUMBER'],
    ['true', 'BOOLEAN'],
    ['FALSE', 'BOOLEAN'],
    ['{"value": "unknown"}', 'OBJECT'],
    ['{}', 'OBJECT'],
  ])('accepts %p as %s', (value, valueType) => {
    expect(validateDefaultValue(value, valueType)).toBeUndefined();
  });

  it.each([
    ['', 'NUMBER'],
    ['abc', 'NUMBER'],
    ['0x10', 'NUMBER'],
    ['yes', 'BOOLEAN'],
    ['', 'BOOLEAN'],
    ['hello', 'OBJECT'],
    ['', 'OBJECT'],
    ['[1, 2]', 'OBJECT'],
    ['null', 'OBJECT'],
    ['"value"', 'OBJECT'],
  ])('rejects %p as %s', (value, valueType) => {
    expect(validateDefaultValue(value, valueType)).toEqual(expect.any(String));
  });
});
