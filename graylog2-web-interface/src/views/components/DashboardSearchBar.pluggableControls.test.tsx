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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';
import MockStore from 'helpers/mocking/StoreMock';
import { SearchActions } from 'views/stores/SearchStore';
import mockAction from 'helpers/mocking/MockAction';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import FormikInput from 'components/common/FormikInput';

import DashboardSearchBar from './DashboardSearchBar';

const testTimeout = applyTimeoutMultiplier(30000);

jest.mock('views/components/DashboardActionsMenu', () => () => <span>View Actions</span>);
jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideStore: MockStore(),
  GlobalOverrideActions: {
    set: jest.fn().mockResolvedValue({}),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(['getInitialState', () => ({ search: { parameters: [] } })]),
  SearchActions: {
    refresh: jest.fn(),
  },
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore(['getInitialState', () => ({ searchesClusterConfig: mockSearchesClusterConfig })]),
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => jest.fn(() => Promise.resolve({
  status: 'OK',
  explanations: [],
})));

describe('DashboardSearchBar pluggable controls', () => {
  const PluggableSearchBarControl = () => {
    return (
      <FormikInput label="Pluggable Control"
                   name="pluggableControl"
                   id="pluggable-control" />
    );
  };

  const mockOnSubmit = jest.fn((_values, entity) => Promise.resolve(entity));
  const mockOnValidate = jest.fn(() => Promise.resolve({}));

  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, {
      'views.components.searchBar': [
        () => ({
          id: 'pluggable-search-bar-control',
          component: PluggableSearchBarControl,
          useInitialSearchValues: () => {
            return ({
              pluggableControl: 'Initial Value',
            });
          },
          useInitialDashboardWidgetValues: () => {
            return ({
              pluggableControl: 'Initial Value',
            });
          },
          onSearchSubmit: mockOnSubmit,
          onDashboardWidgetSubmit: mockOnSubmit,
          validationPayload: (values) => {
            // @ts-ignore
            const { pluggableControl } = values;

            return ({ customKey: pluggableControl });
          },
          onValidate: mockOnValidate,
          placement: 'right',
        }),
      ],
    }));
  });

  beforeEach(() => {
    SearchActions.refresh = mockAction();
    SearchConfigStore.getInitialState = jest.fn(() => ({ searchesClusterConfig: mockSearchesClusterConfig }));
  });

  it('should render and have initial values', async () => {
    render(<DashboardSearchBar />);

    const pluggableFormField = await screen.findByLabelText('Pluggable Control');

    expect(pluggableFormField).toHaveValue('Initial Value');
  });

  it('should register submit handler', async () => {
    render(<DashboardSearchBar />);

    const searchButton = await screen.findByRole('button', { name: /perform search/i });
    await waitFor(() => expect(searchButton).not.toHaveClass('disabled'));
    userEvent.click(searchButton);

    await waitFor(() => expect(mockOnSubmit).toHaveBeenCalledWith(
      {
        pluggableControl: 'Initial Value',
        queryString: '',
        timerange: undefined,
      },
      undefined,
    ));
  }, testTimeout);

  it('should register validation handler', async () => {
    render(<DashboardSearchBar />);

    await waitFor(() => expect(mockOnValidate).toHaveBeenCalledWith({
      pluggableControl: 'Initial Value',
      queryString: '',
      timerange: {},
    }));
  });

  it('should extend query validation payload', async () => {
    render(<DashboardSearchBar />);

    await waitFor(() => expect(validateQuery).toHaveBeenCalledWith({
      customKey: 'Initial Value',
      queryString: '',
      timeRange: undefined,
    }, 'Europe/Berlin'));
  });
});
