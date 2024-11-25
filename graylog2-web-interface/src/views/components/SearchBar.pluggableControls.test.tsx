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
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import { PluginManifest } from 'graylog-web-plugin/plugin';

import { StoreMock as MockStore } from 'helpers/mocking';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';
import FormikInput from 'components/common/FormikInput';
import Query from 'views/logic/queries/Query';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { createSearch } from 'fixtures/searches';
import View from 'views/logic/views/View';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { usePlugin } from 'views/test/testPlugins';
import asMock from 'helpers/mocking/AsMock';
import useSearchConfiguration from 'hooks/useSearchConfiguration';

import OriginalSearchBar from './SearchBar';

const testTimeout = applyTimeoutMultiplier(30000);

jest.mock('hooks/useHotkey', () => jest.fn());
jest.mock('views/logic/fieldtypes/useFieldTypes');

jest.mock('views/hooks/useAutoRefresh');

jest.mock('stores/streams/StreamsStore', () => MockStore(
  ['listStreams', () => ({ then: jest.fn() })],
  'availableStreams',
));

jest.mock('views/components/searchbar/saved-search/SearchActionsMenu', () => jest.fn(() => (
  <div>Saved Search Controls</div>
)));

const mockCurrentQuery = Query.builder()
  .timerange({ type: 'relative', from: 300 })
  .query({ type: 'elasticsearch', query_string: '*' })
  .id('34efae1e-e78e-48ab-ab3f-e83c8611a683')
  .build();

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => jest.fn(() => Promise.resolve({
  status: 'OK',
  explanations: [],
})));

jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

const SearchBar = () => {
  const view = createSearch();
  const { search } = view;
  const viewWithQuery = createSearch()
    .toBuilder()
    .search(search.toBuilder().queries([mockCurrentQuery]).build())
    .build();

  return (
    <TestStoreProvider view={viewWithQuery} initialQuery={mockCurrentQuery.id}>
      <OriginalSearchBar />
    </TestStoreProvider>
  );
};

const PluggableSearchBarControl = () => (
  <FormikInput label="Pluggable Control"
               name="pluggableControl"
               id="pluggable-control" />
);

const mockOnSubmitFromPlugin = jest.fn((_values, _dispatch, entity) => Promise.resolve(entity));
const mockOnValidate = jest.fn(() => Promise.resolve({}));

const testPlugin = new PluginManifest({}, {
  'views.components.searchBar': [
    () => ({
      id: 'pluggable-search-bar-control',
      component: PluggableSearchBarControl,
      useInitialSearchValues: () => ({
        pluggableControl: 'Initial Value',
      }),
      useInitialDashboardWidgetValues: () => ({
        pluggableControl: 'Initial Value',
      }),
      onSearchSubmit: mockOnSubmitFromPlugin,
      onDashboardWidgetSubmit: mockOnSubmitFromPlugin,
      validationPayload: (values) => {
        // @ts-ignore
        const { pluggableControl } = values;

        return ({ customKey: pluggableControl });
      },
      onValidate: mockOnValidate,
      placement: 'right',
    }),
  ],
});

jest.mock('hooks/useSearchConfiguration');

describe('SearchBar pluggable controls', () => {
  useViewsPlugin();
  usePlugin(testPlugin);

  beforeEach(() => {
    asMock(useSearchConfiguration).mockReturnValue({ config: mockSearchesClusterConfig, refresh: () => {} });
  });

  it('should render and have initial values', async () => {
    render(<SearchBar />);

    const pluggableFormField = await screen.findByLabelText('Pluggable Control');

    expect(pluggableFormField).toHaveValue('Initial Value');
  });

  it('should register submit handler which receive current form state and query', async () => {
    render(<SearchBar />);

    const pluggableFormField = await screen.findByLabelText('Pluggable Control');
    userEvent.type(pluggableFormField, '2');

    const searchButton = screen.getByRole('button', {
      name: /perform search \(changes were made after last search execution\)/i,
    });
    await waitFor(() => expect(searchButton).not.toHaveClass('disabled'));
    userEvent.click(searchButton);

    await waitFor(() => expect(mockOnSubmitFromPlugin).toHaveBeenCalledWith(
      {
        pluggableControl: 'Initial Value2',
        queryString: '*',
        streams: [],
        streamCategories: [],
        timerange: { from: 300, type: 'relative' },
      },
      expect.any(Function),
      mockCurrentQuery,
    ));
  }, testTimeout);

  it('should register validation handler', async () => {
    render(<SearchBar />);

    await waitFor(() => expect(mockOnValidate).toHaveBeenCalledWith(
      {
        pluggableControl: 'Initial Value',
        queryString: '*',
        streams: [],
        streamCategories: [],
        timerange: { from: 300, type: 'relative' },
      },
      {
        view: expect.objectContaining({ type: View.Type.Dashboard }),
        executionState: SearchExecutionState.empty(),
      },
    ));
  });

  it('should extend query validation payload', async () => {
    render(<SearchBar />);

    await waitFor(() => expect(validateQuery).toHaveBeenCalledWith({
      customKey: 'Initial Value',
      queryString: '*',
      streams: [],
      streamCategories: [],
      timeRange: { from: 300, type: 'relative' },
    }, 'Europe/Berlin'));
  });
});
