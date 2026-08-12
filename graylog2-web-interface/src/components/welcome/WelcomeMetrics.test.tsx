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
import userEvent from '@testing-library/user-event';
import { defaultUser } from 'defaultMockValues';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import StreamsContext from 'contexts/StreamsContext';
import useCreateSearch from 'views/hooks/useCreateSearch';
import type View from 'views/logic/views/View';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type { Stream } from 'logic/streams/types';
import type { SearchesConfig } from 'components/search/SearchConfig';
import Store from 'logic/local-storage/Store';

import WelcomeMetrics from './WelcomeMetrics';

jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useSearchConfiguration');
jest.mock('views/hooks/useCreateSearch');
jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

const accessibleStream = { id: 'stream-id-1', title: 'Test Stream' } as Stream;

const renderWithStreams = (streams: Array<Stream>) =>
  render(
    <StreamsContext.Provider value={streams}>
      <WelcomeMetrics />
    </StreamsContext.Provider>,
  );

describe('WelcomeMetrics', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useCreateSearch).mockImplementation((viewPromise: Promise<View>) => viewPromise);
    asMock(useSearchConfiguration).mockReturnValue({ config: undefined, refresh: () => {}, isInitialLoading: false });
    asMock(Store.get).mockReturnValue(undefined);
  });

  it('shows Alerts and Events widgets when the user has access to both underlying streams', async () => {
    renderWithStreams([accessibleStream]);

    await screen.findByText('Alerts Today');
    await screen.findByText('Events Today');
  });

  it('shows only Messages Today and Top 5 Sources widgets, without Alerts and Events, when the user is missing access to the alerts/events streams', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());

    renderWithStreams([accessibleStream]);

    await screen.findByText('Messages Today');
    await screen.findByText('Top 5 Sources');
    expect(screen.queryByText('Alerts Today')).not.toBeInTheDocument();
    expect(screen.queryByText('Events Today')).not.toBeInTheDocument();
  });

  it('uses the configured query time range limit as the widgets time range when it is lower than 24 hours', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT1H' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    renderWithStreams([accessibleStream]);

    await screen.findByText('Messages Today');
    expect(await screen.findAllByText('1 hour ago - Now')).not.toHaveLength(0);
  });

  it('falls back to a 24 hour widgets time range when the configured query time range limit is unlimited', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT0S' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    renderWithStreams([accessibleStream]);

    await screen.findByText('Messages Today');
    expect(await screen.findAllByText('1 day ago - Now')).not.toHaveLength(0);
  });

  it('shows a message instead of any widgets when the user has no access to any stream', async () => {
    renderWithStreams([]);

    await screen.findByText('Once you have access to a stream, your message metrics will show up here.');
    expect(screen.queryByText('Messages Today')).not.toBeInTheDocument();
    expect(useCreateSearch).not.toHaveBeenCalled();
  });

  it('allows dismissing the message shown when the user has no access to any stream, persisting the choice', async () => {
    renderWithStreams([]);

    const alert = await screen.findByText('Once you have access to a stream, your message metrics will show up here.');
    const dismissButton = await screen.findByRole('button', { name: /close alert/i });

    await userEvent.click(dismissButton);

    expect(alert).not.toBeInTheDocument();
    expect(Store.set).toHaveBeenCalledWith('welcome-metrics-no-stream-access-dismissed', true);
  });

  it('does not show the message again if it was already dismissed', async () => {
    asMock(Store.get).mockReturnValue(true);

    renderWithStreams([]);

    expect(screen.queryByText('Once you have access to a stream, your message metrics will show up here.')).toBeNull();
  });
});
