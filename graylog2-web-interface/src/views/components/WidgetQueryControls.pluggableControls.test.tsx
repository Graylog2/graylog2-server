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
import WrappingContainer from 'WrappingContainer';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MockStore from 'helpers/mocking/StoreMock';
import Widget from 'views/logic/widgets/Widget';
import mockComponent from 'helpers/mocking/MockComponent';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';

import WidgetContext from './contexts/WidgetContext';
import WidgetQueryControls from './WidgetQueryControls';

import FormikInput from '../../components/common/FormikInput';

const testTimeout = applyTimeoutMultiplier(30000);

jest.mock('hooks/useUserDateTime');
jest.mock('views/components/searchbar/queryvalidation/QueryValidation', () => mockComponent('QueryValidation'));
jest.mock('views/components/searchbar/queryinput/QueryInput', () => ({ value = '' }: { value: string }) => <span>{value}</span>);
jest.mock('hooks/useFeature', () => (key: string) => key === 'search_filter');
jest.mock('views/components/searchbar/queryvalidation/QueryValidation', () => mockComponent('QueryValidation'));
jest.mock('views/components/searchbar/queryinput/BasicQueryInput', () => ({ value = '' }: { value: string }) => <span>{value}</span>);
jest.mock('views/components/searchbar/queryinput/QueryInput', () => ({ value = '' }: { value: string }) => <span>{value}</span>);
jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: jest.fn(),
  },
}));

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    resetTimeRange: jest.fn(() => Promise.resolve()),
    resetQuery: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(
    ['getInitialState', () => ({ search: { parameters: [] } })],
  ),
  SearchActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
  SearchConfigStore: MockStore(['getInitialState', () => ({
    searchesClusterConfig: {
      relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
      query_time_range_limit: 'PT0S',
    },
  })]),
}));

jest.mock('stores/connect', () => {
  const originalModule = jest.requireActual('stores/connect');

  return {
    __esModule: true,
    ...originalModule,
    default: (x) => x,
  };
});

jest.mock('moment', () => {
  const mockMoment = jest.requireActual('moment');

  return Object.assign(() => mockMoment('2019-10-10T12:26:31.146Z'), mockMoment);
});

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => jest.fn(() => Promise.resolve({
  status: 'OK',
  explanations: [],
})));

describe('WidgetQueryControls pluggable controls', () => {
  beforeEach(() => { jest.clearAllMocks(); });

  const config = {
    relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
    query_time_range_limit: 'PT0S',
  };

  const defaultProps = {
    availableStreams: [],
    config,
  };

  const widget = Widget.builder()
    .id('deadbeef')
    .type('dummy')
    .config({})
    .build();

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
          useInitialValues: () => {
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

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <WrappingContainer>
      <WidgetContext.Provider value={widget}>
        {children}
      </WidgetContext.Provider>
    </WrappingContainer>
  );

  const renderSUT = (props = {}) => render(
    <Wrapper>
      <WidgetQueryControls {...defaultProps}
                           {...props} />
    </Wrapper>,
  );

  it('should render and have initial values', async () => {
    renderSUT();

    const pluggableFormField = await screen.findByLabelText('Pluggable Control');

    expect(pluggableFormField).toHaveValue('Initial Value');
  });

  it('should register submit handler which receives current form state', async () => {
    renderSUT();

    const pluggableFormField = await screen.findByLabelText('Pluggable Control');
    userEvent.type(pluggableFormField, '2');

    const searchButton = screen.getByRole('button', {
      name: /perform search \(changes were made after last search execution\)/i,
    });
    await waitFor(() => expect(searchButton).not.toHaveClass('disabled'));
    userEvent.click(searchButton);

    await waitFor(() => expect(mockOnSubmit).toHaveBeenCalledWith({
      pluggableControl: 'Initial Value2',
      queryString: '',
      streams: [],
      timerange: { from: 300, type: 'relative' },
    }));
  }, testTimeout);

  it('should register validation handler', async () => {
    renderSUT();

    await waitFor(() => expect(mockOnValidate).toHaveBeenCalledWith({
      pluggableControl: 'Initial Value',
      queryString: '',
      streams: [],
      timerange: { from: 300, type: 'relative' },
    }));
  });

  it('should extend query validation payload', async () => {
    renderSUT();

    await waitFor(() => expect(validateQuery).toHaveBeenCalledWith({
      customKey: 'Initial Value',
      queryString: '',
      streams: [],
      timeRange: { from: 300, type: 'relative' },
    }));
  });
});
