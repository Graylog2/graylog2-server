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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';

import { StoreMock as MockStore } from 'helpers/mocking';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import QueryResult from 'views/logic/QueryResult';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';

import Sidebar from './Sidebar';

const mockCurrentUser = { timezone: 'UTC' };

jest.mock('stores/users/CurrentUserStore', () => ({
  CurrentUserStore: MockStore(['get', () => mockCurrentUser], ['getInitialState', () => ({ mockCurrentUser })]),
}));

jest.mock('stores/sessions/SessionStore', () => ({ SessionStore: MockStore('isLoggedIn') }));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  rootTimeZone: jest.fn(() => 'America/Chicago'),
  gl2ServerUrl: jest.fn(() => undefined),
}));

jest.mock('hooks/useUserDateTime');

describe('<Sidebar />', () => {
  const viewMetaData = {
    activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
    description: 'A description',
    id: '5b34f4c44880a54df9616380',
    summary: 'query summary',
    title: 'Query Title',
  };
  const effectiveTimerange = {
    type: 'absolute',
    from: '2018-08-28T14:34:26.192Z',
    to: '2018-08-28T14:39:26.192Z',
  } as const;
  const duration = 64;
  const timestamp = '2018-08-28T14:39:26.127Z';
  const query = {
    filter: { type: 'or', filters: [] },
    id: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
    query: { type: 'elasticsearch', query_string: '*' },
    search_types: [],
    timerange: { type: 'relative', from: 300 },
  };
  const errors = [];
  const executionStats = { effective_timerange: effectiveTimerange, duration, timestamp };
  const searchTypes = {};
  const queryResult = new QueryResult({ execution_stats: executionStats, query, errors, search_types: searchTypes });

  const TestComponent = () => <div id="martian">Marc Watney</div>;

  it('should render and open when clicking on header', async () => {
    render(
      <Sidebar viewMetadata={viewMetaData}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText(viewMetaData.title);
  });

  it('should render with a description about the query results', async () => {
    render(
      <Sidebar viewMetadata={viewMetaData}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findAllByText((_content, node) => (node.textContent === 'Query executed in 64ms at 2018-08-28 14:39:26.'));
  });

  const emptyViewMetaData = {
    activeQuery: '34efae1e-e78e-48ab-ab3f-e83c8611a683',
    id: '5b34f4c44880a54df9616380',
  } as ViewMetaData;

  it('should render with a specific default title in the context of a new search', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={emptyViewMetaData}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>,
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText('Unsaved Search');
  });

  it('should render with a specific default title in the context of a new dashboard', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={emptyViewMetaData}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText('Unsaved Dashboard');
  });

  it('should render summary and description of a view', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={viewMetaData}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText(viewMetaData.summary);
    await screen.findByText(viewMetaData.description);
  });

  it('should render placeholder if dashboard has no summary or description', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <Sidebar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText(/This dashboard has no description/);
    await screen.findByText(/This dashboard has no summary/);
  });

  it('should render placeholder if saved search has no summary or description', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={{ ...viewMetaData, description: undefined, summary: undefined }}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText(/This search has no description/);
    await screen.findByText(/This search has no summary/);
  });

  it('should render a summary and description, for a saved search', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={viewMetaData}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText(viewMetaData.summary);
    await screen.findByText(viewMetaData.description);
  });

  it('should not render a summary and description, if the view is an ad hoc search', async () => {
    render(
      <Sidebar viewMetadata={{ ...viewMetaData, id: undefined }}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    fireEvent.click(await screen.findByTitle(/open sidebar/i));

    await screen.findByText('Save the search or export it to a dashboard to add a custom summary and description.');

    expect(screen.queryByText(viewMetaData.summary)).toBeNull();
    expect(screen.queryByText(viewMetaData.description)).toBeNull();
  });

  it('should render widget create options', async () => {
    render(
      <Sidebar viewMetadata={viewMetaData}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    fireEvent.click(await screen.findByLabelText('Create'));

    await screen.findByText('Predefined Aggregation');
  });

  it('should render passed children', async () => {
    render(
      <Sidebar viewMetadata={viewMetaData}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    fireEvent.click(await screen.findByLabelText('Fields'));

    await screen.findByText('Marc Watney');
  });

  it('should close a section when clicking on its title', async () => {
    render(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <Sidebar viewMetadata={viewMetaData}
                 queryId={query.id}
                 results={queryResult}>
          <TestComponent />
        </Sidebar>
      </ViewTypeContext.Provider>,
    );

    fireEvent.click(await screen.findByLabelText('Description'));

    await screen.findByText('Execution');

    fireEvent.click(await screen.findByText('Query Title'));

    expect(screen.queryByText('Execution')).toBeNull();
  });

  it('should close an active section when clicking on its navigation item', async () => {
    render(
      <Sidebar viewMetadata={viewMetaData}
               queryId={query.id}
               results={queryResult}>
        <TestComponent />
      </Sidebar>,
    );

    fireEvent.click(await screen.findByLabelText('Fields'));

    await screen.findByText('Marc Watney');

    fireEvent.click(await screen.findByLabelText('Fields'));

    expect(screen.queryByText('Marc Watney')).toBeNull();
  });
});
