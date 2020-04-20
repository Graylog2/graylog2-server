// @flow strict
import * as React from 'react';
import { cleanup, render, fireEvent, wait } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import asMock from 'helpers/mocking/AsMock';

import { exportSearchMessages, exportSearchTypeMessages } from 'util/MessagesExportUtils';

import type { ViewStateMap } from 'views/logic/views/View';
import Direction from 'views/logic/aggregationbuilder/Direction';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import View, { type ViewType } from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import Widget from 'views/logic/widgets/Widget';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import CSVExportModal, { type Props as CSVExportModalProps } from './CSVExportModal';

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

describe('CSVExportModal', () => {
  const searchType = {
    id: 'search-type-id-1',
    type: 'messages',
  };
  const queries = [
    Query.builder().id('query-id-1').searchTypes([searchType]).build(),
  ];
  const currentSort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending);
  const config = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [currentSort]);
  const widget1 = Widget.builder().id('widget-id-1').type(MessagesWidget.type).config(config)
    .build();
  const widget2 = Widget.builder().id('widget-id-2').type(MessagesWidget.type).config(config)
    .build();
  const states: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.create(),
  });
  const searchWithQueries = Search.builder()
    .id('search-id')
    .queries(queries)
    .build();
  const viewWithoutWidget = (viewType: ViewType) => View.create()
    .toBuilder()
    .id('deadbeef')
    .type(viewType)
    .search(searchWithQueries)
    .state(states)
    .build();
  // Prepare view with one widget
  const statesWithOneWidget: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.builder()
      .widgets(Immutable.List([widget1]))
      .widgetMapping(Immutable.Map({ 'widget-id-1': Immutable.Set(['search-type-id-1']) }))
      .titles(Immutable.Map())
      .build(),
  });
  const viewWithOneWidget = (viewType) => viewWithoutWidget(viewType)
    .toBuilder()
    .state(statesWithOneWidget)
    .build();
  // Prepare view with mulitple widgets
  const statesWithMultipleWidgets: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.builder()
      .widgets(Immutable.List([widget1, widget2]))
      .widgetMapping(Immutable.Map({ 'widget-id-1': Immutable.Set(['search-type-id-1']) }))
      .titles(Immutable.Map())
      .build(),
  });
  const viewWithMultipleWidgets = (viewType) => viewWithoutWidget(viewType)
    .toBuilder()
    .state(statesWithMultipleWidgets)
    .build();
  // Prepare expected payload
  const direction = Direction.Descending;
  const messageSortConfig = new MessageSortConfig('timestamp', direction);
  const payloadSearchMessages = {
    fields_in_order: ['timestamp', 'source', 'message'],
    sort: [messageSortConfig],
    limit: undefined,
    execution_state: new SearchExecutionState(),
  };
  const payloadSearchTypeMessages = {
    ...payloadSearchMessages,
    fields_in_order: ['timestamp', 'source'],
  };

  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  type SimpleCSVExportModalProps = {
    viewType?: ViewType,
    closeModal?: $PropertyType<CSVExportModalProps, 'closeModal'>,
    directExportWidgetId?: $PropertyType<CSVExportModalProps, 'directExportWidgetId'>,
    fields?: $PropertyType<CSVExportModalProps, 'fields'>,
    view?: $PropertyType<CSVExportModalProps, 'view'>,
  };

  const SimpleCSVExportModal = ({ viewType = View.Type.Search, ...props }: SimpleCSVExportModalProps) => (
    <ViewTypeContext.Provider value={viewType}>
      <CSVExportModal view={viewWithoutWidget(viewType)} {...props} />
    </ViewTypeContext.Provider>
  );

  SimpleCSVExportModal.defaultProps = {
    viewType: View.Type.Search,
    closeModal: () => {},
    directExportWidgetId: undefined,
    fields: Immutable.List(),
    view: viewWithoutWidget(View.Type.Search),
  };


  it('should provide current execution state on export', async () => {
    const exportSearchMessagesAction = asMock(exportSearchMessages);
    const parameterBindings = Immutable.Map({ mainSource: new ParameterBinding('value', 'example.org') });
    const effectiveTimeRange = { type: 'absolute', from: '2020-01-01T12:18:17.827Z', to: '2020-01-01T12:23:17.827Z' };
    const globalQuery = { type: 'elasticsearch', query_string: 'source:$mainSource$' };
    const globalOverride = new GlobalOverride(effectiveTimeRange, globalQuery);
    const executionState = new SearchExecutionState(parameterBindings, globalOverride);
    SearchExecutionStateStore.getInitialState.mockReturnValueOnce(executionState);
    const expectedPayload = {
      ...payloadSearchMessages,
      execution_state: executionState,
    };
    const { getByTestId } = render(<SimpleCSVExportModal />);

    const submitButton = getByTestId('csv-download-button');
    fireEvent.click(submitButton);

    await wait(() => expect(exportSearchMessagesAction).toHaveBeenCalledWith(expectedPayload, 'search-id', 'Untitled-Search-search-result'));
  });

  it('should show loading indicator after starting download', async () => {
    const exportSearchMessagesAction = asMock(exportSearchMessages);
    const { getByTestId, getByText } = render(<SimpleCSVExportModal />);

    expect(getByText('Start Download')).not.toBeNull();

    const submitButton = getByTestId('csv-download-button');
    fireEvent.click(submitButton);

    expect(getByText('Downloading...')).not.toBeNull();
    await wait(() => expect(exportSearchMessagesAction).toHaveBeenCalledTimes(1));
  });

  describe('on search page', () => {
    const SearchCSVExportModal = (props) => (
      <SimpleCSVExportModal viewType={View.Type.Search} {...props} />
    );

    it('should not show widget selection when no widget exists', () => {
      const { queryByText } = render(<SearchCSVExportModal />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should not show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export all messages when no widget exists', async () => {
      const exportSearchMessagesAction = asMock(exportSearchMessages);
      const { getByTestId } = render(<SearchCSVExportModal />);

      const submitButton = getByTestId('csv-download-button');
      fireEvent.click(submitButton);

      await wait(() => expect(exportSearchMessagesAction).toHaveBeenCalledTimes(1));
      expect(exportSearchMessagesAction).toHaveBeenCalledWith(payloadSearchMessages, 'search-id', 'Untitled-Search-search-result');
    });

    it('preselect messages widget when only one exists', () => {
      const { queryByText } = render(<SearchCSVExportModal view={viewWithOneWidget(View.Type.Search)} />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export messages related to preselected widget', async () => {
      const { getByTestId } = render(<SearchCSVExportModal view={viewWithOneWidget(View.Type.Search)} />);

      const submitButton = getByTestId('csv-download-button');
      fireEvent.click(submitButton);
      await wait(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));
      expect(exportSearchTypeMessages).toHaveBeenCalledWith(payloadSearchTypeMessages, 'search-id', 'search-type-id-1', 'Untitled-Message-Table-search-result');
    });

    it('show widget selection if more than one exists', () => {
      const { queryByText } = render(<SearchCSVExportModal view={viewWithMultipleWidgets(View.Type.Search)} />);
      expect(queryByText(/Please select a message table to adopt its fields and sort./)).not.toBeNull();
    });

    it('preselect widget on direct export', () => {
      const { queryByText } = render(<SearchCSVExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-id-1" />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export widget messages on direct export', async () => {
      const { getByTestId } = render(<SearchCSVExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-id-1" />);

      const submitButton = getByTestId('csv-download-button');
      fireEvent.click(submitButton);
      await wait(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));
      expect(exportSearchTypeMessages).toHaveBeenCalledWith(payloadSearchTypeMessages, 'search-id', 'search-type-id-1', 'Untitled-Message-Table-search-result');
    });
  });

  describe('on dashboard', () => {
    const DashboardCSVExportModal = (props) => (
      <SimpleCSVExportModal viewType={View.Type.Dashboard} view={viewWithoutWidget(View.Type.Dashboard)} {...props} />
    );

    it('show warning when no messages widget exists', () => {
      const { queryByText } = render(<DashboardCSVExportModal view={viewWithoutWidget(View.Type.Dashboard)} />);
      expect(queryByText('You need to create a message table widget to export its result.')).not.toBeNull();
    });

    it('does not preselect widget when only one exists', () => {
      const { queryByText } = render(<DashboardCSVExportModal view={viewWithOneWidget(View.Type.Dashboard)} />);
      expect(queryByText(/Please select the message table you want to export the search results for./)).not.toBeNull();
    });

    it('show widget selection if more than one exists', () => {
      const { queryByText } = render(<DashboardCSVExportModal view={viewWithMultipleWidgets(View.Type.Dashboard)} />);
      expect(queryByText(/Please select the message table you want to export the search results for./)).not.toBeNull();
    });

    it('preselect widget on direct widget export', () => {
      const { queryByText } = render(<DashboardCSVExportModal view={viewWithMultipleWidgets(View.Type.Dashboard)} directExportWidgetId="widget-id-1" />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/You are currently exporting the search results for the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('should export widget messages on direct export', async () => {
      const { getByTestId } = render(<DashboardCSVExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-id-1" />);

      const submitButton = getByTestId('csv-download-button');
      fireEvent.click(submitButton);
      await wait(() => expect(exportSearchTypeMessages).toHaveBeenCalledTimes(1));
      expect(exportSearchTypeMessages).toHaveBeenCalledWith(payloadSearchTypeMessages, 'search-id', 'search-type-id-1', 'Untitled-Message-Table-search-result');
    });
  });
});
