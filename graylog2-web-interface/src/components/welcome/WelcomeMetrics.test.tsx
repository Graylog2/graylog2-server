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
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import StreamsContext from 'contexts/StreamsContext';
import useCreateSearch from 'views/hooks/useCreateSearch';
import type View from 'views/logic/views/View';
import SearchPage from 'views/pages/SearchPage';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type { Stream } from 'logic/streams/types';
import type { SearchesConfig } from 'components/search/SearchConfig';

import WelcomeMetrics from './WelcomeMetrics';

jest.mock('hooks/usePermissions');
jest.mock('hooks/useSearchConfiguration');
jest.mock('views/hooks/useCreateSearch');
jest.mock('views/pages/SearchPage', () => jest.fn(() => <div>Search Page</div>));

const accessibleStream = { id: 'stream-id-1', title: 'Test Stream' } as Stream;

const renderWithStreams = (streams: Array<Stream>) =>
  render(
    <StreamsContext.Provider value={streams}>
      <WelcomeMetrics />
    </StreamsContext.Provider>,
  );

const lastRenderedView = async () => {
  const viewPromise = asMock(SearchPage).mock.calls.at(-1)[0].view as Promise<View>;

  return viewPromise;
};

const widgetTitlesOfLastRenderedView = async () => {
  const view = await lastRenderedView();
  const widgetTitles = view.state.flatMap((state) => state.titles.get('widget'));

  return widgetTitles.valueSeq().toArray();
};

const widgetIdByTitle = async (title: string) => {
  const view = await lastRenderedView();
  const widgetTitles = view.state.flatMap((state) => state.titles.get('widget'));

  return widgetTitles.findKey((widgetTitle) => widgetTitle === title);
};

const widgetWidthByTitle = async (title: string) => {
  const view = await lastRenderedView();
  const widgetPositions = view.state.flatMap((state) => state.widgetPositions);
  const widgetId = await widgetIdByTitle(title);

  return widgetPositions.get(widgetId).width;
};

const widgetByTitle = async (title: string) => {
  const view = await lastRenderedView();
  const widgets = view.state.valueSeq().flatMap((state) => state.widgets);
  const widgetId = await widgetIdByTitle(title);

  return widgets.find((widget) => widget.id === widgetId);
};

describe('WelcomeMetrics', () => {
  useViewsPlugin();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCreateSearch).mockImplementation((viewPromise: Promise<View>) => viewPromise);
    asMock(useSearchConfiguration).mockReturnValue({ config: undefined, refresh: () => {}, isInitialLoading: false });
  });

  it('shows Alerts and Events widgets when the user has access to both underlying streams', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });

    renderWithStreams([accessibleStream]);

    await waitFor(() => expect(asMock(SearchPage)).toHaveBeenCalled());
    const titles = await widgetTitlesOfLastRenderedView();

    expect(titles).toContain('Alerts Today');
    expect(titles).toContain('Events Today');
    expect(await widgetWidthByTitle('Messages Today')).toBe(4);
  });

  it('shows placeholder widgets explaining missing permissions instead of Alerts and Events widgets when the user is missing access to a stream', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => false, isAnyPermitted: () => false });

    renderWithStreams([accessibleStream]);

    await waitFor(() => expect(asMock(SearchPage)).toHaveBeenCalled());
    const titles = await widgetTitlesOfLastRenderedView();

    expect(titles).toContain('Alerts Today');
    expect(titles).toContain('Events Today');
    expect(titles).toContain('Messages Today');
    expect(titles).toContain('Top 5 Sources');
    expect(await widgetWidthByTitle('Messages Today')).toBe(4);
    expect(await widgetWidthByTitle('Alerts Today')).toBe(4);
    expect(await widgetWidthByTitle('Events Today')).toBe(4);

    const alertsWidget = await widgetByTitle('Alerts Today');
    const eventsWidget = await widgetByTitle('Events Today');

    expect(alertsWidget.type).toBe('text');
    expect(alertsWidget.config.text).toContain('do not have access');
    expect(eventsWidget.type).toBe('text');
    expect(eventsWidget.config.text).toContain('do not have access');
  });

  it('shows an alert instead of widgets when the configured query time range limit is lower than 24 hours', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });
    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT1H' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    renderWithStreams([accessibleStream]);

    expect(await screen.findByText('Metrics unavailable')).toBeInTheDocument();
    expect(screen.queryByText('Search Page')).not.toBeInTheDocument();
    expect(useCreateSearch).not.toHaveBeenCalled();
  });

  it('does not show an alert when the configured query time range limit is unlimited', async () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });
    asMock(useSearchConfiguration).mockReturnValue({
      config: { query_time_range_limit: 'PT0S' } as SearchesConfig,
      refresh: () => {},
      isInitialLoading: false,
    });

    renderWithStreams([accessibleStream]);

    await waitFor(() => expect(asMock(SearchPage)).toHaveBeenCalled());
    expect(screen.queryByText('Metrics unavailable')).not.toBeInTheDocument();
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
