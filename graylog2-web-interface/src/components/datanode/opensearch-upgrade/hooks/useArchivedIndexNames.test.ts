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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import useArchivedIndexNames from './useArchivedIndexNames';

const useArchivedIndexNamesBinding = jest.fn(() => new Set(['graylog_0']));

const archivePlugin = new PluginManifest(
  {},
  {
    'indices.archive': [
      {
        useCanArchive: () => true,
        useArchivedIndexNames: useArchivedIndexNamesBinding,
        archiveAndDeleteIndex: () => Promise.resolve({}),
        isArchiveJobConflict: () => false,
        archiveSystemJobName: 'archive-job',
      },
    ],
  },
);

describe('useArchivedIndexNames', () => {
  afterEach(() => {
    PluginStore.unregister(archivePlugin);
  });

  it('returns an empty set while no plugin provides the indices.archive binding', () => {
    const { result } = renderHook(() => useArchivedIndexNames(['graylog_0'], true));

    expect(result.current.size).toBe(0);
  });

  it('delegates to the registered indices.archive binding', () => {
    PluginStore.register(archivePlugin);

    const { result } = renderHook(() => useArchivedIndexNames(['graylog_0'], true));

    expect(result.current.has('graylog_0')).toBe(true);
    expect(useArchivedIndexNamesBinding).toHaveBeenCalledWith(['graylog_0'], true);
  });
});
