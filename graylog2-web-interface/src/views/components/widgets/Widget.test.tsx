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
import React from 'react';
import * as Immutable from 'immutable';
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import { Map } from 'immutable';
import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';

import Routes from 'routing/Routes';
import { WidgetActions, Widgets } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import WidgetModel from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import { DashboardsStore } from 'views/stores/DashboardsStore';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import SearchActions from 'views/actions/SearchActions';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import ViewState from 'views/logic/views/ViewState';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import { loadDashboard } from 'views/logic/views/Actions';
import { TitlesMap } from 'views/stores/TitleTypes';

import Widget from './Widget';

import WidgetContext from '../contexts/WidgetContext';

jest.mock('views/actions/SearchActions', () => ({
  create: mockAction(jest.fn()),
  get: mockAction(jest.fn()),
}));

jest.mock('views/components/search/IfSearch', () => jest.fn(({ children }) => children));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    update: mockAction(jest.fn()),
    get: mockAction(jest.fn()),
  },
}));

jest.mock('views/logic/views/CopyWidgetToDashboard', () => jest.fn());

jest.mock('../searchbar/QueryInput', () => mockComponent('QueryInput'));
jest.mock('./WidgetHeader', () => 'widget-header');

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: (key) => (key !== 'enterpriseWidgets' ? [] : [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        visualizationComponent: 'dummy-visualization',
        // eslint-disable-next-line react/prop-types
        editComponent: ({ onChange }) => {
          // eslint-disable-next-line react/button-has-type
          return <button onClick={() => onChange({ foo: 23 })}>Click me</button>;
        },
      },
      {
        type: 'default',
        visualizationComponent: () => <span>Unknown widget</span>,
        editComponent: () => <span>Unknown widget in edit mode</span>,
      },
    ]),
  },
}));

jest.mock('views/stores/ChartColorRulesStore', () => ({
  ChartColorRulesStore: {},
}));

jest.mock('views/stores/WidgetStore');
jest.mock('views/stores/TitlesStore');
jest.mock('./WidgetColorContext', () => ({ children }) => children);
jest.mock('views/logic/views/Actions');

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({
      searchesClusterConfig: {},
    }),
  },
  SearchConfigActions: {},
}));

describe('<Widget />', () => {
  const widget = WidgetModel.builder().newId()
    .type('dummy')
    .config({})
    .build();

  const viewState = ViewState.builder().build();
  const query = Query.builder().id('query-id').build();
  const searchSearch = Search.builder().queries([query]).id('search-1').build();
  const search = View.builder()
    .search(searchSearch)
    .type(View.Type.Dashboard)
    .state(Map({ 'query-id': viewState }))
    .id('search-1')
    .title('search 1')
    .build();
  const viewStoreState: ViewStoreState = {
    activeQuery: 'query-id',
    view: search,
    isNew: false,
    dirty: false,
  };

  const searchDB1 = Search.builder().id('search-1').build();
  const dashboard1 = View.builder().type(View.Type.Dashboard).id('view-1').title('view 1')
    .search(searchDB1)
    .build();
  const dashboard2 = View.builder().type(View.Type.Dashboard).id('view-2').title('view 2')
    .build();
  const dashboardList = [dashboard1, dashboard2];
  const dashboardState = {
    list: dashboardList,
    pagination: {
      total: 2,
      page: 1,
      per_page: 10,
      count: 2,
    },
  };

  beforeEach(() => {
    ViewStore.getInitialState = jest.fn(() => viewStoreState);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
  });

  const DummyWidget = (props) => (
    <WidgetContext.Provider value={widget}>
      <Widget widget={widget}
              id="widgetId"
              fields={[]}
              onPositionsChange={() => {}}
              onSizeChange={() => {}}
              title="Widget Title"
              position={new WidgetPosition(1, 1, 1, 1)}
              {...props} />
    </WidgetContext.Provider>
  );

  it('should render with empty props', () => {
    const { baseElement } = render(<DummyWidget />);

    expect(baseElement).toMatchSnapshot();
  });

  it('should render loading widget for widget without data', () => {
    const { queryAllByTestId } = render(<DummyWidget />);

    expect(queryAllByTestId('loading-widget')).toHaveLength(1);
  });

  it('should render error widget for widget with one error', () => {
    const { queryAllByText } = render(<DummyWidget errors={[{ description: 'The widget has failed: the dungeon collapsed, you die!' }]} />);
    const errorWidgets = queryAllByText('The widget has failed: the dungeon collapsed, you die!');

    expect(errorWidgets).toHaveLength(1);
  });

  it('should render error widget including all error messages for widget with multiple errors', () => {
    const { queryAllByText } = render((
      <DummyWidget errors={[
        { description: 'Something is wrong' },
        { description: 'Very wrong' },
      ]} />
    ));

    const errorWidgets1 = queryAllByText('Something is wrong');

    expect(errorWidgets1).toHaveLength(1);

    const errorWidgets2 = queryAllByText('Very wrong');

    expect(errorWidgets2).toHaveLength(1);
  });

  it('should render correct widget visualization for widget with data', () => {
    const { queryAllByTestId, queryAllByTitle } = render(<DummyWidget data={[]} />);

    expect(queryAllByTestId('loading-widget')).toHaveLength(0);
    expect(queryAllByTitle('Widget Title')).toHaveLength(2);
  });

  it('renders placeholder if widget type is unknown', async () => {
    const unknownWidget = WidgetModel.builder()
      .id('widgetId')
      .type('i-dont-know-this-widget-type')
      .config({})
      .build();
    const UnknownWidget = (props) => (
      <Widget widget={unknownWidget}
              id="widgetId"
              fields={[]}
              onPositionsChange={() => {}}
              onSizeChange={() => {}}
              title="Widget Title"
              position={new WidgetPosition(1, 1, 1, 1)}
              {...props} />

    );
    const { findByText } = render(
      <UnknownWidget data={[]} />,
    );

    await findByText('Unknown widget');
  });

  it('renders placeholder in edit mode if widget type is unknown', async () => {
    const unknownWidget = WidgetModel.builder()
      .newId()
      .type('i-dont-know-this-widget-type')
      .config({})
      .build();
    const UnknownWidget = (props) => (
      <WidgetContext.Provider value={unknownWidget}>
        <Widget widget={unknownWidget}
                editing
                id="widgetId"
                fields={[]}
                onPositionsChange={() => {}}
                onSizeChange={() => {}}
                title="Widget Title"
                position={new WidgetPosition(1, 1, 1, 1)}
                {...props} />
      </WidgetContext.Provider>
    );
    const { findByText } = render(
      <UnknownWidget data={[]} />,
    );

    await findByText('Unknown widget in edit mode');
  });

  it('copies title when duplicating widget', async () => {
    const { getByTestId, getByText } = render(<DummyWidget title="Dummy Widget" />);

    const actionToggle = getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);
    const duplicateBtn = getByText('Duplicate');

    WidgetActions.duplicate = mockAction(jest.fn().mockResolvedValue(WidgetModel.builder().id('duplicatedWidgetId').build()));

    TitlesActions.set = mockAction(jest.fn().mockResolvedValue(Immutable.Map() as TitlesMap));

    fireEvent.click(duplicateBtn);

    await waitFor(() => expect(WidgetActions.duplicate).toHaveBeenCalled());
    await waitFor(() => expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, 'duplicatedWidgetId', 'Dummy Widget (copy)'));
  });

  it('adds cancel action to widget in edit mode', () => {
    const { queryAllByText } = render(<DummyWidget editing />);
    const cancel = queryAllByText('Cancel');

    expect(cancel).toHaveLength(1);
  });

  it('does not trigger action when clicking cancel after no changes were made', () => {
    const { getByText } = render(<DummyWidget editing />);

    WidgetActions.updateConfig = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));

    const cancelBtn = getByText('Cancel');

    fireEvent.click(cancelBtn);

    expect(WidgetActions.updateConfig).not.toHaveBeenCalled();
  });

  it('restores original state of widget config when clicking cancel after changes were made', () => {
    const widgetWithConfig = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({ foo: 42 })
      .build();
    const { getByText } = render(<DummyWidget editing widget={widgetWithConfig} />);

    WidgetActions.updateConfig = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    WidgetActions.update = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    const onChangeBtn = getByText('Click me');

    fireEvent.click(onChangeBtn);

    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = getByText('Cancel');

    fireEvent.click(cancelButton);

    expect(WidgetActions.update).toHaveBeenCalledWith('widgetId', widgetWithConfig);
  });

  it('does not restore original state of widget config when clicking "Finish Editing"', () => {
    const widgetWithConfig = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({ foo: 42 })
      .build();
    const { getByText } = render(<DummyWidget editing widget={widgetWithConfig} />);

    WidgetActions.updateConfig = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    WidgetActions.update = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    const onChangeBtn = getByText('Click me');

    fireEvent.click(onChangeBtn);

    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const saveButton = getByText('Save');

    fireEvent.click(saveButton);

    expect(WidgetActions.update).not.toHaveBeenCalledWith('widgetId', { config: { foo: 42 }, id: 'widgetId', type: 'dummy' });
  });

  it('does not display export to CSV action if widget is not a message table', () => {
    const dummyWidget = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({})
      .build();
    const { getByTestId, queryByText } = render(<DummyWidget title="Dummy Widget" widget={dummyWidget} />);

    const actionToggle = getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);

    expect(queryByText('Export to CSV')).toBeNull();
  });

  it('allows export to CSV for message tables', () => {
    const messagesWidget = MessagesWidget.builder()
      .id('widgetId')
      .config({})
      .build();

    const { getByTestId, getByText } = render(<DummyWidget title="Dummy Widget" widget={messagesWidget} />);

    const actionToggle = getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);

    const exportButton = getByText('Export to CSV');

    fireEvent.click(exportButton);

    expect(getByText('Export message table search results to CSV')).not.toBeNull();
  });

  describe('copy widget to dashboard', () => {
    beforeEach(() => {
      // @ts-ignore
      DashboardsStore.getInitialState = jest.fn(() => dashboardState);
      ViewManagementActions.get = mockAction(jest.fn((async () => Promise.resolve(dashboard1.toJSON()))));
      SearchActions.get = mockAction(jest.fn(() => Promise.resolve(searchDB1.toJSON())));
      ViewManagementActions.update = mockAction(jest.fn((view) => Promise.resolve(view)));
      SearchActions.create = mockAction(jest.fn(() => Promise.resolve({ search: searchDB1 })));
      Routes.pluginRoute = jest.fn((route) => (id) => `${route}-${id}`);

      asMock(CopyWidgetToDashboard).mockImplementation(() => View.builder()
        .search(Search.builder().id('search-id').build())
        .id('new-id').type(View.Type.Dashboard)
        .build());
    });

    const renderAndClick = () => {
      const { getByText, getByTestId } = render(<DummyWidget />);
      const actionToggle = getByTestId('widgetActionDropDown');

      fireEvent.click(actionToggle);
      const copyToDashboard = getByText('Copy to Dashboard');

      fireEvent.click(copyToDashboard);
      const view1ListItem = getByText('view 1');

      fireEvent.click(view1ListItem);
      const selectBtn = getByText('Select');

      fireEvent.click(selectBtn);
    };

    it('should get dashboard from backend', async () => {
      renderAndClick();
      await waitFor(() => expect(ViewManagementActions.get).toHaveBeenCalledTimes(1));

      expect(ViewManagementActions.get).toHaveBeenCalledWith('view-1');
    });

    it('should get corresponding search to dashboard', async () => {
      renderAndClick();
      await waitFor(() => expect(SearchActions.get).toHaveBeenCalledTimes(1));

      expect(SearchActions.get).toHaveBeenCalledWith('search-1');
    });

    it('should create new search for dashboard', async () => {
      renderAndClick();
      await waitFor(() => expect(SearchActions.create).toHaveBeenCalledTimes(1));

      expect(SearchActions.create).toHaveBeenCalledWith(Search.builder().id('search-id').parameters([]).queries([])
        .build());
    });

    it('should update dashboard with new search and widget', async () => {
      renderAndClick();
      await waitFor(() => expect(ViewManagementActions.update).toHaveBeenCalledTimes(1));

      expect(ViewManagementActions.update).toHaveBeenCalledWith(
        View.builder()
          .search(Search.builder().id('search-1').build())
          .id('new-id').type(View.Type.Dashboard)
          .build(),
      );
    });

    it('should redirect to updated dashboard', async () => {
      renderAndClick();
      await waitFor(() => expect(loadDashboard).toHaveBeenCalled());

      expect(loadDashboard).toHaveBeenCalledWith('view-1');
    });
  });
});
