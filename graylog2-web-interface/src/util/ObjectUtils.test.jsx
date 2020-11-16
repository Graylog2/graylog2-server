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
import ObjectUtils from 'util/ObjectUtils';

describe('ObjectUtils', () => {
  describe('ObjectUtils#clone()', () => {
    it('should clone the given object', () => {
      const a = { hello: 'world' };
      const b = ObjectUtils.clone(a);

      expect(a.hello).toEqual('world');
      expect(b.hello).toEqual('world');

      b.hello = 'nope';

      expect(a.hello).toEqual('world');
      expect(b.hello).toEqual('nope');
    });
  });

  describe('ObjectUtils#isEmpty()', () => {
    it('should return true for empty objects', () => {
      expect(ObjectUtils.isEmpty({})).toEqual(true);
    });

    it('should return false for non-empty objects', () => {
      expect(ObjectUtils.isEmpty({ hello: 'world' })).toEqual(false);
    });
  });
});
