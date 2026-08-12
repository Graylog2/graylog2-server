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
import { render, waitFor, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';
import StreamsContext from 'contexts/StreamsContext';
import useCreateSearch from 'views/hooks/useCreateSearch';
import type View from 'views/logic/views/View';
import SearchPage from 'views/pages/SearchPage';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type { Stream } from 'logic/streams/types';

import WelcomeMetrics from './WelcomeMetrics';

jest.mock('hooks/usePermissions');
jest.mock('views/hooks/useCreateSearch');
jest.mock('views/pages/SearchPage', () => jest.fn(() => <div>Search Page</div>));

const accessibleStream = { id: 'stream-id-1', title: 'Test Stream' } as Stream;

const renderWithStreams = (streams: Array<Stream>) =>
  render(
    <StreamsContext.Provider value={streams}>
      <WelcomeMetrics />
    </StreamsContext.Provider>,
  );

const widgetTitlesOfLastRenderedView = async () => {
  const viewPromise = asMock(SearchPage).mock.calls.at(-1)[0].view as Promise<View>;
  const view = await viewPromise;
  const widgetTitles = view.state.flatMap((state) => state.titles.get('widget'));

  return widgetTitles.valueSeq().toArray();
};

describe('WelcomeMetrics', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCreateSearch).mockImplementation((viewPromise: Promise<View>) => viewPromise);
  });

  it('shows Alerts and Events widgets when the user has access to both underlying streams', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });

    renderWithStreams([accessibleStream]);

    await waitFor(() => expect(asMock(SearchPage)).toHaveBeenCalled());
    const titles = await widgetTitlesOfLastRenderedView();

    expect(titles).toContain('Alerts Today');
    expect(titles).toContain('Events Today');
  });

  it('hides Alerts and Events widgets when the user is missing access to a stream', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => false, isAnyPermitted: () => false });

    renderWithStreams([accessibleStream]);

    await waitFor(() => expect(asMock(SearchPage)).toHaveBeenCalled());
    const titles = await widgetTitlesOfLastRenderedView();

    expect(titles).not.toContain('Alerts Today');
    expect(titles).not.toContain('Events Today');
    expect(titles).toContain('Messages Today');
    expect(titles).toContain('Top 5 Sources');
  });

  it('shows a message instead of any widgets when the user has no access to any stream', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => false, isAnyPermitted: () => false });

    renderWithStreams([]);

    expect(
      await screen.findByText('Once you have access to a stream, your message metrics will show up here.'),
    ).toBeInTheDocument();
    expect(screen.queryByText('Search Page')).not.toBeInTheDocument();
    expect(useCreateSearch).not.toHaveBeenCalled();
  });
});
