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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import asMock from 'helpers/mocking/AsMock';
import selectEvent from 'react-select-event';
import { Optional } from 'utility-types';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';

import { TitleType } from 'views/stores/TitleTypes';
import { exportSearchMessages, exportSearchTypeMessages } from 'util/MessagesExportUtils';
import type { ViewStateMap } from 'views/logic/views/View';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { AbsoluteTimeRange, ElasticsearchQueryString } from 'views/logic/queries/Query';
import View, { ViewType } from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import {
  messagesWidget,
  stateWithOneWidget, viewWithMultipleWidgets,
  viewWithOneWidget,
  viewWithoutWidget,
} from 'views/components/export/Fixtures';

import ExportModal, { Props as ExportModalProps } from './ExportModal';
import { createWidget } from 'views/logic/WidgetTestHelpers';

jest.mock('util/MessagesExportUtils', () => ({
  exportSearchMessages: jest.fn(() => Promise.resolve()),
  exportSearchTypeMessages: jest.fn(() => Promise.resolve()),
}));

const MockSearchExecutionState = new SearchExecutionState();

jest.mock('views/stores/SearchExecutionStateStore', () => ({
  SearchExecutionStateStore: {
    getInitialState: jest.fn(() => MockSearchExecutionState),
    listen: () => jest.fn(),
  },
}));

const pluginExports: PluginRegistration = {
  exports: {
    enterpriseWidgets: [createWidget('messages')],
    'views.export.formats': [{
      type: 'csv',
      displayName: () => 'CSV',
      mimeType: 'text/csv',
      fileExtension: 'csv',
    }],
  },
};

describe('ExportModal', () => {
  // Prepare expected payload
  const payload = {
    fields_in_order: ['level', 'http_method', 'message'],
    limit: undefined,
    execution_state: new SearchExecutionState(),
  };

  beforeAll(() => {
    PluginStore.register(pluginExports);
  });

  afterAll(() => {
    PluginStore.unregister(pluginExports);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  type SimpleExportModalProps = {
    viewType?: ViewType,
  } & Optional<ExportModalProps>;

  const SimpleExportModal = ({ viewType = View.Type.Search, ...props }: SimpleExportModalProps) => (
    <ViewTypeContext.Provider value={viewType}>
      <ExportModal view={viewWithoutWidget(viewType)} {...props as ExportModalProps} />
    </ViewTypeContext.Provider>
  );

  SimpleExportModal.defaultProps = {
    viewType: View.Type.Search,
    closeModal: () => {},
    directExportWidgetId: undefined,
    fields: Immutable.List(),
    view: viewWithoutWidget(View.Type.Search),
  };

  it('should provide current execution state on export', async () => {
    const parameterBindings = Immutable.Map({ mainSource: new ParameterBinding('value', 'example.org') });
    const effectiveTimeRange: AbsoluteTimeRange = { type: 'absolute', from: '2020-01-01T12:18:17.827Z', to: '2020-01-01T12:23:17.827Z' };
    const globalQuery: ElasticsearchQueryString = { type: 'elasticsearch', query_string: 'source:$mainSource$' };
    const globalOverride = new GlobalOverride(effectiveTimeRange, globalQuery);
    const executionState = new SearchExecutionState(parameterBindings, globalOverride);

    asMock(SearchExecutionStateStore.getInitialState).mockReturnValueOnce(executionState);
    const expectedPayload = {
      ...payload,
      fields_in_order: [
        'timestamp',
        'source',
        'message',
      ],
      execution_state: executionState,
    };
    const { getByTestId } = render(<SimpleExportModal />);

    const submitButton = getByTestId('download-button');

    fireEvent.click(submitButton);

    await waitFor(() => expect(exportSearchMessages).toHaveBeenCalledWith(
      expectedPayload,
      'search-id',
      'text/csv',
      'Untitled-Search-search-result.csv',
    ));
  });

  it('should show loading indicator after starting download', async () => {
    const { getByTestId, findByText, getAllByText } = render(<SimpleExportModal />);

    expect(getAllByText('Start Download')).toHaveLength(2);

    const submitButton = getByTestId('download-button');

    fireEvent.click(submitButton);

    await findByText('Downloading...');

    await waitFor(() => expect(exportSearchMessages).toHaveBeenCalledTimes(1));
  });

  it('should be closed after finishing download', async () => {
    const closeModalStub = jest.fn();
    const { getByTestId } = render(<SimpleExportModal closeModal={closeModalStub} />);

    const submitButton = getByTestId('download-button');

    fireEvent.click(submitButton);

    await waitFor(() => expect(closeModalStub).toHaveBeenCalledTimes(1));
  });

  it('initial fields should not contain the message field if message list config showMessageRow is false', async () => {
    const widgetConfig = new MessagesWidgetConfig(['level', 'http_method'], false, [], []);
    const widgetWithoutMessageRow = messagesWidget().toBuilder().config(widgetConfig).build();
    const viewStateMap: ViewStateMap = Immutable.Map({
      'query-id-1': stateWithOneWidget(messagesWidget()).toBuilder()
        .widgets(Immutable.List([widgetWithoutMessageRow]))
        .build(),
    });
    const view = viewWithoutWidget(View.Type.Search)
      .toBuilder()
      .state(viewStateMap)
      .build();
    const { getByTestId } = render(<SimpleExportModal view={view} />);

    const submitButton = getByTestId('download-button');

    fireEvent.click(submitButton);

    await waitFor(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));

    expect(exportSearchTypeMessages).toHaveBeenCalledWith(
      {
        ...payload,
        fields_in_order: [
          'level',
          'http_method',
        ],
      },
      'search-id',
      'search-type-id-1',
      'text/csv',
      'Widget-1-search-result.csv',
    );
  });

  describe('on search page', () => {
    const SearchExportModal = (props) => (
      <SimpleExportModal viewType={View.Type.Search} {...props} />
    );

    it('should not show widget selection when no widget exists', () => {
      const { queryByText } = render(<SearchExportModal />);

      // should not show widget selection but settings form
      expect(queryByText(/Define the fields for your file./)).not.toBeNull();
      // should not show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export all messages with default fields when no widget exists', async () => {
      const { getByTestId } = render(<SearchExportModal />);

      const submitButton = getByTestId('download-button');

      fireEvent.click(submitButton);

      await waitFor(() => expect(exportSearchMessages).toHaveBeenCalledTimes(1));

      expect(exportSearchMessages).toHaveBeenCalledWith(
        {
          ...payload,
          fields_in_order: [
            'timestamp',
            'source',
            'message',
          ],
        },
        'search-id',
        'text/csv',
        'Untitled-Search-search-result.csv',
      );
    });

    it('preselect messages widget when only one exists', () => {
      const { queryByText } = render(<SearchExportModal view={viewWithOneWidget(View.Type.Search)} />);

      // should not show widget selection but settings form
      expect(queryByText(/Define the fields for your file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export messages related to preselected widget', async () => {
      const { getByTestId } = render(<SearchExportModal view={viewWithOneWidget(View.Type.Search)} />);

      const submitButton = getByTestId('download-button');

      fireEvent.click(submitButton);
      await waitFor(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));

      expect(exportSearchTypeMessages).toHaveBeenCalledWith(
        payload,
        'search-id',
        'search-type-id-1',
        'text/csv',
        'Widget-1-search-result.csv',
      );
    });

    it('show widget selection if more than one exists', async () => {
      const { getByLabelText, queryByText } = render(<SearchExportModal view={viewWithMultipleWidgets(View.Type.Search)} />);

      const select = getByLabelText('Select message table');

      expect(queryByText(/Please select a message table to adopt its fields./)).not.toBeNull();

      await selectEvent.openMenu(select);

      expect(queryByText('Widget 1')).not.toBeNull();
      expect(queryByText('Widget 2')).not.toBeNull();
    });

    it('preselect widget on direct export', () => {
      const { queryByText } = render(<SearchExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-id-1" />);

      // should not show widget selection but settings form
      expect(queryByText(/Define the fields for your file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export widget messages on direct export', async () => {
      const { getByTestId } = render(<SearchExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-id-1" />);

      const submitButton = getByTestId('download-button');

      fireEvent.click(submitButton);
      await waitFor(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));

      expect(exportSearchTypeMessages).toHaveBeenCalledWith(
        payload,
        'search-id',
        'search-type-id-1',
        'text/csv',
        'Widget-1-search-result.csv',
      );
    });
  });

  describe('on dashboard', () => {
    const DashboardExportModal = (props) => (
      <SimpleExportModal viewType={View.Type.Dashboard} view={viewWithoutWidget(View.Type.Dashboard)} {...props} />
    );

    it('show warning when no messages widget exists', () => {
      const { queryByText } = render(<DashboardExportModal view={viewWithoutWidget(View.Type.Dashboard)} />);

      expect(queryByText('You need to create a message table widget to export its result.')).not.toBeNull();
    });

    it('does not preselect widget when only one exists', () => {
      const { queryByText } = render(<DashboardExportModal view={viewWithOneWidget(View.Type.Dashboard)} />);

      expect(queryByText(/Please select the message table you want to export the search results for./)).not.toBeNull();
    });

    it('show widget selection if more than one exists', async () => {
      const { queryByText, getByLabelText } = render(<DashboardExportModal view={viewWithMultipleWidgets(View.Type.Dashboard)} />);
      const select = getByLabelText('Select message table');

      expect(queryByText(/Please select the message table you want to export the search results for./)).not.toBeNull();

      await selectEvent.openMenu(select);

      expect(queryByText('Widget 1')).not.toBeNull();
      expect(queryByText('Widget 2')).not.toBeNull();
    });

    it('show widget selection with widgets from all dashboard pages', async () => {
      const secondViewState: ViewState = ViewState.builder()
        .widgets(Immutable.List([messagesWidget('widget-id-2')]))
        .widgetMapping(Immutable.Map({ 'widget-id-2': Immutable.Set(['search-type-id-2']) }))
        .titles(Immutable.Map<TitleType, Immutable.Map<string, string>>({ widget: Immutable.Map({ 'widget-id-2': 'Widget 2' }) }))
        .build();

      const complexView = viewWithoutWidget(View.Type.Dashboard)
        .toBuilder()
        .state(Immutable.Map({ 'query-id-1': stateWithOneWidget(messagesWidget()), 'query-id-2': secondViewState }))
        .build();

      const { queryByText, getByLabelText } = render(<DashboardExportModal view={complexView} />);
      const select = getByLabelText('Select message table');

      await selectEvent.openMenu(select);

      expect(queryByText('Widget 1')).not.toBeNull();
      expect(queryByText('Widget 2')).not.toBeNull();
    });

    it('preselect widget on direct widget export', () => {
      const { queryByText } = render(<DashboardExportModal view={viewWithMultipleWidgets(View.Type.Dashboard)} directExportWidgetId="widget-id-1" />);

      // should not show widget selection but settings form
      expect(queryByText(/Define the fields for your file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/You are currently exporting the search results for the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export widget messages on direct export', async () => {
      const { getByTestId } = render(<DashboardExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-id-1" />);

      const submitButton = getByTestId('download-button');

      fireEvent.click(submitButton);
      await waitFor(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));

      expect(exportSearchTypeMessages).toHaveBeenCalledWith(
        payload,
        'search-id',
        'search-type-id-1',
        'text/csv',
        'Widget-1-search-result.csv',
      );
    });
  });
});
