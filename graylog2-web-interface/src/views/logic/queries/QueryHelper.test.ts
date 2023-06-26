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
import { concatQueryStrings, escape } from './QueryHelper';

describe('QueryHelper', () => {
  it('quotes $ in values', () => {
    expect(escape('foo$bar$')).toEqual('foo\\$bar\\$');
  });

  describe('concatQueryStrings', () => {
    it('concat queries by default with AND and brackets', () => {
      const result = concatQueryStrings(['field1:value1 OR field2:value2', 'field3:value3']);

      expect(result).toEqual('(field1:value1 OR field2:value2) AND (field3:value3)');
    });

    it('concat queries with custom operator and without brackets', () => {
      const result = concatQueryStrings(['field1:value1 OR field2:value2', 'field3:value3'], { operator: 'OR', withBrackets: false });

      expect(result).toEqual('field1:value1 OR field2:value2 OR field3:value3');
    });

    it('filtrate empty query strings', () => {
      const result = concatQueryStrings(['     ', 'field1:value1 OR field2:value2', ' ', 'field3:value3', '']);

      expect(result).toEqual('(field1:value1 OR field2:value2) AND (field3:value3)');
    });

    it('doesnt wrap in brackets if we have only one query string', () => {
      const result = concatQueryStrings(['     ', 'field1:value1 OR field2:value2', ' ', '']);

      expect(result).toEqual('field1:value1 OR field2:value2');
    });

    it('handles null query strings', () => {
      const result = concatQueryStrings([undefined, 'field1:value1 OR field2:value2', null, '']);

      expect(result).toEqual('field1:value1 OR field2:value2');
    });
  });
});
