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
import * as React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import useEventDefinitionOverviewSections from './useEventDefinitionOverviewSections';

describe('useEventDefinitionOverviewSections', () => {
  let manifest: PluginManifest;

  afterEach(() => {
    if (manifest) PluginStore.unregister(manifest);
  });

  it('returns an empty array when no extensions are registered', () => {
    const { result } = renderHook(() => useEventDefinitionOverviewSections());
    expect(result.current).toEqual([]);
  });

  it('returns registered components sorted by `order`', () => {
    const A = () => <div>A</div>;
    const B = () => <div>B</div>;
    manifest = new PluginManifest(
      {},
      {
        'eventDefinitions.components.overviewPageSections': [
          { key: 'b', component: B, order: 20 },
          { key: 'a', component: A, order: 10 },
        ],
      },
    );
    PluginStore.register(manifest);

    const { result } = renderHook(() => useEventDefinitionOverviewSections());
    expect(result.current.map((e) => e.key)).toEqual(['a', 'b']);
  });
});
