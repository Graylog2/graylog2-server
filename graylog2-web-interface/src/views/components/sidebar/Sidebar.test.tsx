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

import View from 'views/logic/views/View';
import QueryResult from 'views/logic/QueryResult';
import { asMock } from 'helpers/mocking';
import useViewType from 'views/hooks/useViewType';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useViewTitle from 'views/hooks/useViewTitle';
import useViewMetadata from 'views/hooks/useViewMetadata';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import GlobalOverride from 'views/logic/search/GlobalOverride';

import Sidebar from './Sidebar';

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  rootTimeZone: jest.fn(() => 'America/Chicago'),
  gl2ServerUrl: jest.fn(() => undefined),
  isCloud: jest.fn(() => false),
  isFeatureEnabled: jest.fn(() => false),
}));

jest.mock('hooks/useHotkey', () => jest.fn());
jest.mock('views/hooks/useViewType');
jest.mock('views/hooks/useActiveQueryId');
jest.mock('views/hooks/useViewTitle');
jest.mock('views/hooks/useViewMetadata');
jest.mock('views/hooks/useGlobalOverride');

describe('<Sidebar />', () => {
  const queryId = '34efae1e-e78e-48ab-ab3f-e83c8611a683';
  const viewMetaData = {
    activeQuery: queryId,
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
  const timestamp = '2018-08-28T14:39:27.127Z';
  const query = {
    filter: { type: 'or', filters: [] },
    id: queryId,
    query: { type: 'elasticsearch', query_string: '*' },
    search_types: [],
    timerange: { type: 'relative', from: 300 },
  };
  const errors = [];
  const executionStats = { effective_timerange: effectiveTimerange, duration, timestamp };
  const searchTypes = { 'search-type-id': { total: 12345, type: 'generic' } };
  const queryResult = new QueryResult({ execution_stats: executionStats, query, errors, search_types: searchTypes });

  const openDescriptionSection = async () => fireEvent.click(await screen.findByRole('button', { name: /description/i }));

  const TestComponent = () => <div id="martian">Marc Watney</div>;

  const renderSidebar = () => render(
    <TestStoreProvider>
      <Sidebar results={queryResult}>
        <TestComponent />
      </Sidebar>
    </TestStoreProvider>,
  );

  useViewsPlugin();

  beforeEach(() => {
    asMock(useViewType).mockReturnValue(View.Type.Search);
    asMock(useActiveQueryId).mockReturnValue(queryId);
    asMock(useViewMetadata).mockReturnValue(viewMetaData);
    asMock(useGlobalOverride).mockReturnValue(GlobalOverride.empty());
  });

  it('should render with a description about the query results', async () => {
    renderSidebar();

    await openDescriptionSection();

    await screen.findAllByText((_content, node) => (node.textContent === 'Query executed in 64ms at 2018-08-28 16:39:27'));
  });

  it('should render summary and description of a view', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Dashboard);

    renderSidebar();

    await openDescriptionSection();

    await screen.findByText(viewMetaData.summary);
    await screen.findByText(viewMetaData.description);
  });

  it('should render placeholder if dashboard has no summary or description', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Dashboard);
    asMock(useViewMetadata).mockReturnValue({ ...viewMetaData, description: undefined, summary: undefined });

    renderSidebar();

    await openDescriptionSection();

    await screen.findByText(/This dashboard has no description/);
    await screen.findByText(/This dashboard has no summary/);
  });

  it('should render placeholder if saved search has no summary or description', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Search);
    asMock(useViewMetadata).mockReturnValue({ ...viewMetaData, description: undefined, summary: undefined });

    renderSidebar();

    await openDescriptionSection();

    await screen.findByText(/This search has no description/);
    await screen.findByText(/This search has no summary/);
  });

  it('should render a summary and description, for a saved search', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Search);

    renderSidebar();

    await openDescriptionSection();

    await screen.findByText(viewMetaData.summary);
    await screen.findByText(viewMetaData.description);
  });

  it('should not render a summary and description, if the view is an ad hoc search', async () => {
    asMock(useViewMetadata).mockReturnValue({ ...viewMetaData, id: undefined });

    renderSidebar();

    await openDescriptionSection();

    await screen.findByText('Save the search or export it to a dashboard to add a custom summary and description.');

    expect(screen.queryByText(viewMetaData.summary)).toBeNull();
    expect(screen.queryByText(viewMetaData.description)).toBeNull();
  });

  it('should render the effective search execution time range for searches', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Search);
    renderSidebar();

    await openDescriptionSection();

    await screen.findByText('2018-08-28 16:34:26.192');
    await screen.findByText('2018-08-28 16:39:26.192');
    await screen.findByText(/total results *12,345/i);
  });

  it('should not render the effective search execution time range and total result for dashboards without global override', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Dashboard);
    renderSidebar();

    fireEvent.click(await screen.findByRole('button', { name: /description/i }));

    await screen.findByText((_content, node) => (node.textContent === 'Effective time rangeVaries per widget'));
    await screen.findByText((_content, node) => (node.textContent === 'Total resultsVaries per widget'));
  });

  it('should render the effective search execution time range for dashboards with global override', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Dashboard);

    asMock(useGlobalOverride).mockReturnValue({
      timerange: {
        type: 'relative',
        from: 300,
      },
    } as GlobalOverride);

    renderSidebar();

    fireEvent.click(await screen.findByRole('button', { name: /description/i }));

    await screen.findByText('2018-08-28 16:34:26.192');
    await screen.findByText('2018-08-28 16:39:26.192');
  });

  it('should render widget create options', async () => {
    renderSidebar();

    fireEvent.click(await screen.findByRole('button', { name: /open create section/i }));

    await screen.findByText('Predefined Aggregation');
  });

  it('should render passed children', async () => {
    renderSidebar();

    fireEvent.click(await screen.findByRole('button', { name: /open fields section/i }));

    await screen.findByText('Marc Watney');
  });

  it('should close a section when clicking on its title', async () => {
    asMock(useViewType).mockReturnValue(View.Type.Search);
    asMock(useViewTitle).mockReturnValue(viewMetaData.title);

    renderSidebar();

    fireEvent.click(await screen.findByRole('button', { name: /open description section/i }));

    await screen.findByText('Execution');

    fireEvent.click(await screen.findByText('Query Title'));

    expect(screen.queryByText('Execution')).toBeNull();
  });

  it('should close an active section when clicking on its navigation item', async () => {
    renderSidebar();

    fireEvent.click(await screen.findByRole('button', { name: /open fields section/i }));

    await screen.findByText('Marc Watney');

    fireEvent.click(await screen.findByRole('button', { name: /close fields section/i }));

    expect(screen.queryByText('Marc Watney')).toBeNull();
  });
});
