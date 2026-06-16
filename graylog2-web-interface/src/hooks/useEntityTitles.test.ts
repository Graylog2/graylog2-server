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
import { waitFor } from 'wrappedTestingLibrary';

import { SystemCatalog } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';

import useEntityTitles from './useEntityTitles';

jest.mock('@graylog/server-api', () => ({
  SystemCatalog: {
    getTitles: jest.fn(),
  },
}));

describe('useEntityTitles', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('resolves titles by id and exposes not-permitted ids', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({
      entities: [
        { id: 'stream-a', type: 'streams', title: 'Stream A' },
        { id: 'stream-b', type: 'streams', title: 'Stream B' },
      ],
      not_permitted_to_view: ['stream-c'],
    });

    const { result } = renderHook(() =>
      useEntityTitles([
        { id: 'stream-a', type: 'streams' },
        { id: 'stream-b', type: 'streams' },
        { id: 'stream-c', type: 'streams' },
      ]),
    );

    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));

    expect(result.current.titlesById).toEqual({
      'stream-a': 'Stream A',
      'stream-b': 'Stream B',
    });
    expect(result.current.notPermittedIds.has('stream-c')).toBe(true);
  });

  it('calls SystemCatalog.getTitles with the sorted entity list', async () => {
    asMock(SystemCatalog.getTitles).mockResolvedValue({ entities: [], not_permitted_to_view: [] });

    renderHook(() => useEntityTitles([{ id: 'stream-a', type: 'streams' }]));

    await waitFor(() => expect(SystemCatalog.getTitles).toHaveBeenCalled());

    expect(SystemCatalog.getTitles).toHaveBeenCalledWith({
      entities: [{ id: 'stream-a', type: 'streams' }],
    });
  });

  it('does not call the API when the entities list is empty', () => {
    renderHook(() => useEntityTitles([]));

    expect(SystemCatalog.getTitles).not.toHaveBeenCalled();
  });
});
