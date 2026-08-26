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
import userEvent from '@testing-library/user-event';

import { Events } from '@graylog/server-api';

import TagsFilter from 'components/events/TagsFilter';

jest.mock('@graylog/server-api', () => ({
  Events: { filterOptions: jest.fn() },
}));

jest.mock('routing/useQuery', () => ({
  __esModule: true,
  default: () => ({}),
}));

const mockedFilterOptions = Events.filterOptions as jest.MockedFunction<typeof Events.filterOptions>;

const filterOptionsResponse = (tags: Array<string>) =>
  ({ tags }) as unknown as Awaited<ReturnType<typeof Events.filterOptions>>;

const tagsAttribute = {
  id: 'tags',
  title: 'Tags',
  type: 'STRING',
  filterable: true,
} as const;

const renderFilter = () =>
  render(
    <TagsFilter
      attribute={tagsAttribute as never}
      allActiveFilters={undefined}
      filter={undefined}
      filterValueRenderer={(_v, t) => t}
      onSubmit={jest.fn()}
    />,
  );

describe('TagsFilter', () => {
  beforeEach(() => {
    mockedFilterOptions.mockReset();
  });

  it('requests the tags filter options and renders returned values', async () => {
    mockedFilterOptions.mockResolvedValue(filterOptionsResponse(['exfil', 'phishing']));

    renderFilter();

    await waitFor(() => {
      expect(mockedFilterOptions).toHaveBeenCalledWith(expect.objectContaining({ fields: ['tags'] }));
    });

    expect(await screen.findByText('exfil')).toBeInTheDocument();
    expect(await screen.findByText('phishing')).toBeInTheDocument();
  });

  it('renders an empty list when no tags are returned', async () => {
    mockedFilterOptions.mockResolvedValue(filterOptionsResponse([]));

    renderFilter();

    await waitFor(() => {
      expect(mockedFilterOptions).toHaveBeenCalled();
    });

    expect(screen.queryByText('phishing')).not.toBeInTheDocument();
  });

  it('forwards the typed search query to the server', async () => {
    mockedFilterOptions.mockResolvedValue(filterOptionsResponse(['credential-access']));

    renderFilter();

    await waitFor(() => {
      expect(mockedFilterOptions).toHaveBeenCalledWith(expect.objectContaining({ field_query: '' }));
    });

    await userEvent.type(await screen.findByPlaceholderText('Search for tags'), 'access');

    // The search box debounces for 1s before the query is forwarded.
    await waitFor(
      () => {
        expect(mockedFilterOptions).toHaveBeenCalledWith(expect.objectContaining({ field_query: 'access' }));
      },
      { timeout: 5000 },
    );
  }, 10000);

  it('falls back to empty suggestions when the request fails', async () => {
    mockedFilterOptions.mockRejectedValue(new Error('boom'));

    renderFilter();

    await waitFor(() => {
      expect(mockedFilterOptions).toHaveBeenCalled();
    });

    expect(screen.queryByText('phishing')).not.toBeInTheDocument();
  });
});
