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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { Events } from '@graylog/server-api';

import TagsFilter from 'components/events/TagsFilter';

jest.mock('@graylog/server-api', () => ({
  Events: { slices: jest.fn() },
}));

jest.mock('routing/useQuery', () => ({
  __esModule: true,
  default: () => ({}),
}));

const mockedSlices = Events.slices as jest.MockedFunction<typeof Events.slices>;

const tagsAttribute = {
  id: 'tags',
  title: 'Tags',
  type: 'STRING',
  filterable: true,
} as const;

describe('TagsFilter', () => {
  beforeEach(() => {
    mockedSlices.mockReset();
  });

  it('queries Events.slices with slice_column=tags and renders returned values', async () => {
    mockedSlices.mockResolvedValue({
      slices: [
        { value: 'phishing', count: 5, title: null, meta: {} },
        { value: 'exfil', count: 2, title: null, meta: {} },
      ],
    } as unknown as Awaited<ReturnType<typeof Events.slices>>);

    render(
      <TagsFilter
        attribute={tagsAttribute as never}
        allActiveFilters={undefined}
        filter={undefined}
        filterValueRenderer={(_v, t) => t}
        onSubmit={jest.fn()}
      />,
    );

    await waitFor(() => {
      expect(mockedSlices).toHaveBeenCalledWith(expect.objectContaining({ slice_column: 'tags', include_all: true }));
    });

    expect(await screen.findByText('exfil')).toBeInTheDocument();
    expect(await screen.findByText('phishing')).toBeInTheDocument();
  });

  it('renders an empty list when slices returns nothing', async () => {
    mockedSlices.mockResolvedValue({ slices: [] } as unknown as Awaited<ReturnType<typeof Events.slices>>);

    render(
      <TagsFilter
        attribute={tagsAttribute as never}
        allActiveFilters={undefined}
        filter={undefined}
        filterValueRenderer={(_v, t) => t}
        onSubmit={jest.fn()}
      />,
    );

    await waitFor(() => {
      expect(mockedSlices).toHaveBeenCalled();
    });

    expect(screen.queryByText('phishing')).not.toBeInTheDocument();
  });

  it('falls back to empty suggestions when the request fails', async () => {
    mockedSlices.mockRejectedValue(new Error('boom'));

    render(
      <TagsFilter
        attribute={tagsAttribute as never}
        allActiveFilters={undefined}
        filter={undefined}
        filterValueRenderer={(_v, t) => t}
        onSubmit={jest.fn()}
      />,
    );

    await waitFor(() => {
      expect(mockedSlices).toHaveBeenCalled();
    });

    expect(screen.queryByText('phishing')).not.toBeInTheDocument();
  });
});
