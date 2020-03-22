// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import Widget from 'views/logic/widgets/widget';
import ViewState from 'views/logic/views/ViewState';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { ViewStateMap } from 'views/logic/views/View';

import CSVExportModal from './CSVExportModal';

describe('CSVExportModal', () => {
  const queries = [
    Query.builder().id('query-id-1').build(),
  ];
  const currentSort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending);
  const config = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [currentSort]);
  const widget1 = Widget.builder().id('widget-1-id').type(MessagesWidget.type).config(config)
    .build();
  const widget2 = Widget.builder().id('widget-2-id').type(MessagesWidget.type).config(config)
    .build();
  const states: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.create(),
  });
  const searchWithQueries = Search.builder()
    .queries(queries)
    .build();
  const viewWithoutWidget = viewType => View.create()
    .toBuilder()
    .id('deadbeef')
    .type(viewType)
    .search(searchWithQueries)
    .state(states)
    .build();
  // Prepare view with one widget
  const statesWithOneWidget: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.builder().widgets(Immutable.List([widget1])).titles(Immutable.Map()).build(),
  });
  const viewWithOneWidget = viewType => viewWithoutWidget(viewType)
    .toBuilder()
    .state(statesWithOneWidget)
    .build();
  // Prepare view with mulitple widgets
  const statesWithMultipleWidgets: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.builder().widgets(Immutable.List([widget1, widget2])).titles(Immutable.Map()).build(),
  });
  const viewWithMultipleWidgets = viewType => viewWithoutWidget(viewType)
    .toBuilder()
    .state(statesWithMultipleWidgets)
    .build();

  afterEach(cleanup);

  describe('on search page', () => {
    const SimpleCSVExportModal = props => (
      <ViewTypeContext.Provider value={View.Type.Search}>
        <CSVExportModal view={viewWithoutWidget(View.Type.Search)} fields={Immutable.List()} closeModal={() => {}} {...props} />
      </ViewTypeContext.Provider>
    );

    it('allow download when no messages widget exists', () => {
      const { queryByText } = render(<SimpleCSVExportModal />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should not show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('preselect messages widget when only one exists', () => {
      const { queryByText } = render(<SimpleCSVExportModal view={viewWithOneWidget(View.Type.Search)} />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });

    it('show widget selection if more than one exists', () => {
      const { queryByText } = render(<SimpleCSVExportModal view={viewWithMultipleWidgets(View.Type.Search)} />);
      expect(queryByText(/Please select a message table to adopt its fields and sort./)).not.toBeNull();
    });

    it('should preselect widget on direct download', () => {
      const { queryByText } = render(<SimpleCSVExportModal view={viewWithMultipleWidgets(View.Type.Search)} directExportWidgetId="widget-1-id" />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/The following settings are based on the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });
  });

  describe('on dashboard', () => {
    const SimpleCSVExportModal = props => (
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <CSVExportModal view={viewWithoutWidget(View.Type.Dashboard)} fields={Immutable.List()} closeModal={() => {}} {...props} />
      </ViewTypeContext.Provider>
    );

    it('show warning when no messages widget exists', () => {
      const { queryByText } = render(<SimpleCSVExportModal />);
      expect(queryByText('You need to create a message table widget to export its result.')).not.toBeNull();
    });

    it('does not preselect widget when only one exists', () => {
      const { queryByText } = render(<SimpleCSVExportModal view={viewWithOneWidget(View.Type.Dashboard)} />);
      expect(queryByText(/Please select the message table you want to export the search results for./)).not.toBeNull();
    });

    it('show widget selection if more than one exists', () => {
      const { queryByText } = render(<SimpleCSVExportModal view={viewWithMultipleWidgets(View.Type.Dashboard)} />);
      expect(queryByText(/Please select the message table you want to export the search results for./)).not.toBeNull();
    });

    it('should preselect widget on direct download', () => {
      const { queryByText } = render(<SimpleCSVExportModal view={viewWithMultipleWidgets(View.Type.Dashboard)} directExportWidgetId="widget-1-id" />);
      // should not show widget selection but settings form
      expect(queryByText(/Define the fields and sorting for your CSV file./)).not.toBeNull();
      // should show info about selected widget
      expect(queryByText(/You are currently exporting the search results for the message table:/)).not.toBeNull();
      // should not allow widget selection
      expect(queryByText('Select different message table')).toBeNull();
    });
  });
});
