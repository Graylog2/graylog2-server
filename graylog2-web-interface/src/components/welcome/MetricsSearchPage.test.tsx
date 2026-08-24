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
import { render, screen } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useCreateSearch from 'views/hooks/useCreateSearch';
import type View from 'views/logic/views/View';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type { SearchesConfig } from 'components/search/SearchConfig';

import GeneralWelcomeMetrics from './GeneralWelcomeMetrics';

jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useSearchConfiguration');
jest.mock('views/hooks/useCreateSearch');

describe('MetricsSearchPage', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useCreateSearch).mockImplementation((viewPromise: Promise<View>) => viewPromise);
    asMock(useSearchConfiguration).mockReturnValue({ config: undefined, refresh: () => {}, isInitialLoading: false });
  });

  it('shows Alerts and Events widgets when the user has access to both underlying streams', async () => {
    render(<GeneralWelcomeMetrics />);

    await screen.findByText('Alerts Today');
    await screen.findByText('Events Today');
  });

  it('shows only Messages Today and Top 5 Sources widgets, without Alerts and Events, when the user is missing access to both the alerts and events streams', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());

    render(<GeneralWelcomeMetrics />);

    await screen.findByText('Messages Today');
    await screen.findByText('Top 5 Sources');
    expect(screen.queryByText('Alerts Today')).not.toBeInTheDocument();
    expect(screen.queryByText('Events Today')).not.toBeInTheDocument();
  });

  it('still shows Alerts and Events widgets when the user only has access to one of the two underlying streams', async () => {
    asMock(useCurrentUser).mockReturnValue(
      defaultUser.toBuilder().permissions(['streams:read:000000000000000000000002']).build(),
    );

    render(<GeneralWelcomeMetrics />);

    await screen.findByText('Alerts Today');
    await screen.findByText('Events Today');
  });

  it('uses the configured query time range limit as the widgets time range when it is lower than 24 hours', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT1H' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    render(<GeneralWelcomeMetrics />);

    await screen.findByText('Messages Today');
    expect(await screen.findAllByText('1 hour ago - Now')).not.toHaveLength(0);
  });

  it('falls back to a 24 hour widgets time range when the configured query time range limit is unlimited', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT0S' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    render(<GeneralWelcomeMetrics />);

    await screen.findByText('Messages Today');
    expect(await screen.findAllByText('1 day ago - Now')).not.toHaveLength(0);
  });

  it('shows only the Top 5 Sources widget when topSourcesOnly is set, even with full stream access', async () => {
    render(<GeneralWelcomeMetrics topSourcesOnly />);

    await screen.findByText('Top 5 Sources');
    expect(screen.queryByText('Messages Today')).not.toBeInTheDocument();
    expect(screen.queryByText('Alerts Today')).not.toBeInTheDocument();
    expect(screen.queryByText('Events Today')).not.toBeInTheDocument();
  });
});
