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

import { renderHook } from 'wrappedTestingLibrary/hooks';
import { PluginStore, PluginManifest } from 'graylog-web-plugin/plugin';

import useShowRouteForEntity from 'routing/hooks/useShowRouteForEntity';
import Routes from 'routing/Routes';

describe('getShowRouteFromGRN', () => {
  describe('should return correct route', () => {
    it.each`
      id              | type               | entityShowURL
      ${'user-id'}    | ${'user'}          | ${Routes.SYSTEM.USERS.show('user-id')}
      ${'stream-id'}  | ${'stream'}        | ${Routes.stream_search('stream-id')}
    `('for $type with id $id', ({ id, type, entityShowURL }) => {
      const { result } = renderHook(() => useShowRouteForEntity(id, type));

      expect(result.current).toBe(entityShowURL);
    });
  });

  describe('with plugin data', () => {
    const plugin = new PluginManifest({}, {
      entityRoutes: [
        (id, type) => (type === 'dashboard' && id === 'special-id' ? '/plugin-entity-route' : null),
      ],
    });

    beforeAll(() => PluginStore.register(plugin));

    afterAll(() => PluginStore.unregister(plugin));

    it('should return entity routes defined by plugins', () => {
      const { result } = renderHook(() => useShowRouteForEntity('special-id', 'dashboard'));

      expect(result.current).toBe('/plugin-entity-route');
    });

    it('should still work with common routes', () => {
      const { result } = renderHook(() => useShowRouteForEntity('common-id', 'dashboard'));

      expect(result.current).toBe('/dashboards/common-id');
    });
  });
});
