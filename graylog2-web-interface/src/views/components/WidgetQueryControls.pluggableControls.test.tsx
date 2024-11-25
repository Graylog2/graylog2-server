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
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import { PluginManifest } from 'graylog-web-plugin/plugin';

import Widget from 'views/logic/widgets/Widget';
import mockComponent from 'helpers/mocking/MockComponent';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import TestStoreProvider from 'views/test/TestStoreProvider';
import type Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { usePlugin } from 'views/test/testPlugins';

import WidgetContext from './contexts/WidgetContext';
import WidgetQueryControls from './WidgetQueryControls';

import FormikInput from '../../components/common/FormikInput';

const testTimeout = applyTimeoutMultiplier(30000);

jest.mock('views/components/searchbar/queryvalidation/QueryValidation', () => mockComponent('QueryValidation'));

jest.mock('views/components/searchbar/queryinput/QueryInput');
jest.mock('views/components/searchbar/queryinput/BasicQueryInput');
jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

jest.mock('hooks/useSearchConfiguration', () => () => ({
  config: {
    relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
    query_time_range_limit: 'PT0S',
  },
  refresh: () => {},
}));

jest.mock('moment', () => {
  const mockMoment = jest.requireActual('moment');

  return Object.assign(() => mockMoment('2019-10-10T12:26:31.146Z'), mockMoment);
});

jest.mock('views/components/searchbar/queryvalidation/validateQuery', () => jest.fn(() => Promise.resolve({
  status: 'OK',
  explanations: [],
})));

jest.mock('views/hooks/useGlobalOverride');

jest.mock('views/logic/slices/createSearch', () => (s: Search) => s);

const PluggableSearchBarControl = () => (
  <FormikInput label="Pluggable Control"
               name="pluggableControl"
               id="pluggable-control" />
);

const mockOnSubmit = jest.fn((_values, _dispatch, entity) => Promise.resolve(entity));
const mockOnValidate = jest.fn(() => Promise.resolve({}));

const testPlugin = new PluginManifest({}, {
  'views.components.searchBar': [
    () => ({
      id: 'pluggable-search-bar-control',
      component: PluggableSearchBarControl,
      useInitialDashboardWidgetValues: () => ({
        pluggableControl: 'Initial Value',
      }),
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
});

describe('WidgetQueryControls pluggable controls', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

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

  useViewsPlugin();
  usePlugin(testPlugin);

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <TestStoreProvider>
      <WidgetContext.Provider value={widget}>
        {children}
      </WidgetContext.Provider>
    </TestStoreProvider>
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

  it('should register submit handler which receives current form state and widget', async () => {
    renderSUT();

    const pluggableFormField = await screen.findByLabelText('Pluggable Control');
    userEvent.type(pluggableFormField, '2');

    const searchButton = screen.getByRole('button', {
      name: /perform search \(changes were made after last search execution\)/i,
    });
    await waitFor(() => expect(searchButton).not.toHaveClass('disabled'));
    userEvent.click(searchButton);

    await waitFor(() => expect(mockOnSubmit).toHaveBeenCalledWith(
      {
        pluggableControl: 'Initial Value2',
        queryString: '',
        streams: undefined,
        timerange: { from: 300, type: 'relative' },
      },
      expect.any(Function),
      widget,
    ));
  }, testTimeout);

  it('should register validation handler', async () => {
    renderSUT();

    await waitFor(() => expect(mockOnValidate).toHaveBeenCalledWith(
      {
        pluggableControl: 'Initial Value',
        queryString: '',
        streams: undefined,
        timerange: { from: 300, type: 'relative' },
      },
      {
        view: expect.objectContaining({ type: View.Type.Dashboard }),
        executionState: SearchExecutionState.empty(),
      },
    ));
  });

  it('should extend query validation payload', async () => {
    renderSUT();

    await waitFor(() => expect(validateQuery).toHaveBeenCalledWith({
      customKey: 'Initial Value',
      queryString: '',
      streams: undefined,
      timeRange: { from: 300, type: 'relative' },
    }, 'Europe/Berlin'));
  });
});
