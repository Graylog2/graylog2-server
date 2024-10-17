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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import { events as eventsFixtures } from 'fixtures/events';
import asMock from 'helpers/mocking/AsMock';
import useAppDispatch from 'stores/useAppDispatch';
import { finishedLoading } from 'views/logic/slices/searchExecutionSlice';
import SearchResult from 'views/logic/SearchResult';
import reexecuteSearchTypes from 'views/components/widgets/reexecuteSearchTypes';
import type { SearchErrorResponse } from 'views/logic/SearchError';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';

import EventsList from './EventsList';

jest.mock('views/hooks/useAutoRefresh');

const dummySearchJobResults = {
  errors: [],
  execution: { cancelled: false, completed_exceptionally: false, done: true },
  id: 'foo',
  owner: 'me',
  search_id: 'bar',
  results: {},
};
jest.mock('views/components/widgets/reexecuteSearchTypes');
jest.mock('stores/useAppDispatch');

describe('EventsList', () => {
  const config = EventsWidgetConfig.createDefault();

  const data = {
    id: 'search-type-id',
    type: 'events' as const,
    events: eventsFixtures,
    totalResults: 1,
  };

  useViewsPlugin();

  beforeEach(() => {
    asMock(useAutoRefresh).mockReturnValue({
      refreshConfig: null,
      startAutoRefresh: () => {},
      stopAutoRefresh: () => {},
      restartAutoRefresh: () => {},
      animationId: 'animation-id',
    });
  });

  const clickNextPageButton = () => {
    const paginationListItem = screen.getByRole('listitem', { name: /next/i });

    const nextPageButton = within(paginationListItem).getByRole('button');
    userEvent.click(nextPageButton);
  };

  const SimpleEventsList = ({ data: _data = data, config: _config = config, fields = Immutable.List([]), ...props }: Partial<React.ComponentProps<typeof EventsList>>) => (
    <TestStoreProvider>
      <EventsList title="Events List"
                  editing={false}
                  filter=""
                  onConfigChange={() => Promise.resolve()}
                  type="events"
                  id="events-list"
                  queryId="deadbeef"
                  toggleEdit={() => {}}
                  setLoadingState={() => {}}
                  data={_data}
                  config={_config}
                  fields={fields}
                  height={480}
                  width={640}
                  {...props} />
    </TestStoreProvider>
  );

  it('lists events', async () => {
    render(<SimpleEventsList data={data} />);

    await screen.findByRole('cell', { name: /test event 1/i });
  });

  it('reexecute query for search type, when using pagination', async () => {
    const dispatch = jest.fn().mockResolvedValue(finishedLoading({
      result: new SearchResult(dummySearchJobResults),
      widgetMapping: Immutable.Map(),
    }));
    asMock(useAppDispatch).mockReturnValue(dispatch);
    const searchTypePayload = { [data.id]: { page: 2, per_page: 10 } };
    const secondPageSize = 10;

    render(<SimpleEventsList data={{ ...data, totalResults: 10 + secondPageSize }} />);

    clickNextPageButton();

    await waitFor(() => expect(reexecuteSearchTypes).toHaveBeenCalledWith(searchTypePayload));
  });

  it('disables refresh actions, when using pagination', async () => {
    const stopAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      refreshConfig: null,
      startAutoRefresh: () => {},
      stopAutoRefresh,
      restartAutoRefresh: () => {},
      animationId: 'animation-id',
    });

    const dispatch = jest.fn().mockResolvedValue(finishedLoading({
      result: new SearchResult(dummySearchJobResults),
      widgetMapping: Immutable.Map(),
    }));
    asMock(useAppDispatch).mockReturnValue(dispatch);
    const secondPageSize = 10;

    render(<SimpleEventsList data={{ ...data, totalResults: 10 + secondPageSize }} />);

    clickNextPageButton();

    await waitFor(() => expect(stopAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('displays error description, when using pagination throws an error', async () => {
    const dispatch = jest.fn().mockResolvedValue(finishedLoading({
      result: new SearchResult({
        ...dummySearchJobResults,
        errors: [{
          description: 'Error description',
        } as SearchErrorResponse],
      }),
      widgetMapping: Immutable.Map(),
    }));
    asMock(useAppDispatch).mockReturnValue(dispatch);

    const secondPageSize = 10;

    render(<SimpleEventsList data={{ ...data, totalResults: 10 + secondPageSize }} />);

    clickNextPageButton();

    await screen.findByText('Error description');
  });
});
