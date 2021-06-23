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
import URI from 'urijs';

let Routes;
const prefix = '/test';

describe('Routes', () => {
  describe('without prefix', () => {
    beforeAll(() => {
      jest.resetModules();
      window.appConfig = {}; // Ensure no prefix is set
      Routes = jest.requireActual('./Routes').default;
    });

    it('returns a route from constant', () => {
      expect(Routes.SEARCH).toMatch('/search');
    });

    it('returns a route from function', () => {
      expect(Routes.node('id')).toMatch('/system/nodes/id');
    });

    it('routes contain query parameters', () => {
      const uri = URI(Routes.search('', { rangetype: 'relative', relative: 300 }, 'hour'));

      expect(uri.path()).toMatch('/search');
      expect(uri.hasQuery('q', '')).toBeTruthy();
      expect(uri.hasQuery('rangetype', 'relative')).toBeTruthy();
      expect(uri.hasQuery('relative', '300')).toBeTruthy();
      expect(uri.hasQuery('interval', 'hour')).toBeTruthy();
    });
  });

  describe('with prefix', () => {
    beforeAll(() => {
      jest.resetModules();

      window.appConfig = {
        gl2AppPathPrefix: prefix,
      };

      Routes = jest.requireActual('./Routes').default;
    });

    it('returns a route from constant', () => {
      expect(Routes.SEARCH).toMatch(`${prefix}/search`);
    });

    it('returns a route from function', () => {
      expect(Routes.node('id')).toMatch(`${prefix}/system/nodes/id`);
    });

    it('routes contain query parameters', () => {
      const uri = URI(Routes.search('', { rangetype: 'relative', relative: 300 }, 'hour'));

      expect(uri.path()).toMatch(`${prefix}/search`);
      expect(uri.hasQuery('q', '')).toBeTruthy();
      expect(uri.hasQuery('rangetype', 'relative')).toBeTruthy();
      expect(uri.hasQuery('relative', '300')).toBeTruthy();
      expect(uri.hasQuery('interval', 'hour')).toBeTruthy();
    });
  });
});
