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
import { act } from 'react';

import useTrackedIncompatibleIndices from './useTrackedIncompatibleIndices';

import type { IncompatibleIndexRow, IncompatibleIndicesResponse } from '../fetchIncompatibleIndices';

const makeIndex = (overrides: Partial<IncompatibleIndexRow>): IncompatibleIndexRow => ({
  id: 'index',
  index_name: 'index',
  version: '7.10.2',
  warm_index: false,
  managed_index: false,
  system_index: false,
  active_write_index: null,
  begin: null,
  end: null,
  ...overrides,
});

const makeResponse = (list: Array<IncompatibleIndexRow>): IncompatibleIndicesResponse => ({
  list,
  pagination: { total: list.length },
  attributes: [],
});

describe('useTrackedIncompatibleIndices', () => {
  it('exposes loaded rows as trackedIndices and flags when data has loaded', () => {
    const { result } = renderHook(() => useTrackedIncompatibleIndices());
    const loaded = makeIndex({ id: 'graylog_0', index_name: 'graylog_0' });

    expect(result.current.hasLoaded).toBe(false);

    act(() => result.current.onDataLoaded(makeResponse([loaded])));

    expect(result.current.trackedIndices).toEqual([loaded]);
    expect(result.current.hasLoaded).toBe(true);
  });

  it('keeps a selected index across page changes and refreshes its attributes', () => {
    const { result } = renderHook(() => useTrackedIncompatibleIndices());
    const asWriteIndex = makeIndex({ id: 'graylog_5', index_name: 'graylog_5', active_write_index: 'set-1' });
    const afterRotation = makeIndex({ id: 'graylog_5', index_name: 'graylog_5', active_write_index: null });

    act(() => result.current.onDataLoaded(makeResponse([asWriteIndex])));
    act(() => result.current.onChangeSelection(['graylog_5'], [asWriteIndex]));
    act(() => result.current.onDataLoaded(makeResponse([afterRotation])));

    expect(result.current.selectedIndices).toEqual([afterRotation]);

    act(() => result.current.onDataLoaded(makeResponse([makeIndex({ id: 'other', index_name: 'other' })])));

    expect(result.current.selectedIndices).toEqual([afterRotation]);
  });

  it('drops deselected indices', () => {
    const { result } = renderHook(() => useTrackedIncompatibleIndices());
    const first = makeIndex({ id: 'legacy_0', index_name: 'legacy_0' });
    const second = makeIndex({ id: 'legacy_1', index_name: 'legacy_1' });

    act(() => result.current.onDataLoaded(makeResponse([first, second])));
    act(() => result.current.onChangeSelection(['legacy_0', 'legacy_1'], [first, second]));

    expect(result.current.selectedIndices.map(({ id }) => id)).toEqual(['legacy_0', 'legacy_1']);

    act(() => result.current.onChangeSelection(['legacy_1'], [second]));

    expect(result.current.selectedIndices).toEqual([second]);
  });

  it('does not merge an unselected index whose name looks like a property path', () => {
    const { result } = renderHook(() => useTrackedIncompatibleIndices());
    const selected = makeIndex({ id: 'graylog_0', index_name: 'graylog_0' });
    const dotted = makeIndex({ id: 'graylog_0.id', index_name: 'graylog_0.id' });

    act(() => result.current.onDataLoaded(makeResponse([selected])));
    act(() => result.current.onChangeSelection(['graylog_0'], [selected]));
    act(() => result.current.onDataLoaded(makeResponse([dotted])));

    expect(result.current.selectedIndices.map(({ id }) => id)).toEqual(['graylog_0']);
  });
});
