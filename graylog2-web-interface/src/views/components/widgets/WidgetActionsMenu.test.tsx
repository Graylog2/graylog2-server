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
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';
import { Map } from 'immutable';
import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';

import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import WidgetModel from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import { DashboardsStore } from 'views/stores/DashboardsStore';
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

import WidgetActionsMenu from './WidgetActionsMenu';

import WidgetContext from '../contexts/WidgetContext';
import WidgetFocusContext, { WidgetFocusContextType } from '../contexts/WidgetFocusContext';

jest.mock('views/components/search/IfSearch', () => jest.fn(({ children }) => children));

jest.mock('views/logic/views/CopyWidgetToDashboard', () => jest.fn());

jest.mock('views/stores/ChartColorRulesStore', () => ({
  ChartColorRulesStore: {},
}));

jest.mock('views/logic/views/Actions');

describe('<Widget />', () => {
  const widget = WidgetModel.builder().newId()
    .type('dummy')
    .id('widget-id')
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

  type DummyWidgetProps = {
    widget?: WidgetModel,
    focusedWidget?: WidgetFocusContextType['focusedWidget'],
    setWidgetFocusing?: WidgetFocusContextType['setWidgetFocusing'],
    setWidgetEditing?: WidgetFocusContextType['setWidgetEditing'],
    title?: string
    isFocused?: boolean,
  }

  const DummyWidget = ({ widget: propsWidget = widget, setWidgetFocusing = () => {}, setWidgetEditing = () => {}, focusedWidget, ...props }: DummyWidgetProps) => (
    <WidgetFocusContext.Provider value={{ setWidgetFocusing, setWidgetEditing, focusedWidget }}>
      <WidgetContext.Provider value={propsWidget}>
        <WidgetActionsMenu isFocused={false}
                           toggleEdit={() => {}}
                           title="Widget Title"
                           view={viewStoreState}
                           position={new WidgetPosition(1, 1, 1, 1)}
                           onPositionsChange={() => {}}
                           {...props} />
      </WidgetContext.Provider>
    </WidgetFocusContext.Provider>
  );

  it('is updating widget focus context on focus', () => {
    const mockSetWidgetFocusing = jest.fn();
    render(<DummyWidget title="Dummy Widget" setWidgetFocusing={mockSetWidgetFocusing} />);

    const focusButton = screen.getByTitle('Focus this widget');

    fireEvent.click(focusButton);

    expect(mockSetWidgetFocusing).toHaveBeenCalledWith('widget-id');
  });

  it('is updating widget focus context on un-focus', () => {
    const mockSetWidgetFocusing = jest.fn();
    render(<DummyWidget title="Dummy Widget" isFocused setWidgetFocusing={mockSetWidgetFocusing} />);

    const unfocusButton = screen.getByTitle('Un-focus widget');

    fireEvent.click(unfocusButton);

    expect(mockSetWidgetFocusing).toHaveBeenCalledWith(undefined);
  });

  it('copies title when duplicating widget', async () => {
    render(<DummyWidget title="Dummy Widget" />);

    const actionToggle = screen.getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);
    const duplicateBtn = screen.getByText('Duplicate');

    WidgetActions.duplicate = mockAction(jest.fn().mockResolvedValue(WidgetModel.builder().id('duplicatedWidgetId').build()));

    TitlesActions.set = mockAction(jest.fn().mockResolvedValue(Immutable.Map() as TitlesMap));

    fireEvent.click(duplicateBtn);

    await waitFor(() => expect(WidgetActions.duplicate).toHaveBeenCalled());
    await waitFor(() => expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, 'duplicatedWidgetId', 'Dummy Widget (copy)'));
  });

  it('does not display export action if widget is not a message table', () => {
    const dummyWidget = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({})
      .build();
    const { getByTestId, queryByText } = render(<DummyWidget title="Dummy Widget" widget={dummyWidget} />);

    const actionToggle = getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);

    expect(queryByText('Export')).toBeNull();
  });

  it('allows export for message tables', () => {
    const messagesWidget = MessagesWidget.builder()
      .id('widgetId')
      .config({})
      .build();

    render(<DummyWidget title="Dummy Widget" widget={messagesWidget} />);

    const actionToggle = screen.getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);

    const exportButton = screen.getByText('Export');

    fireEvent.click(exportButton);

    expect(screen.getByText('Export message table search results')).not.toBeNull();
  });

  describe('copy widget to dashboard', () => {
    beforeEach(() => {
      // @ts-ignore
      DashboardsStore.getInitialState = jest.fn(() => dashboardState);
      ViewManagementActions.get = mockAction(jest.fn((async () => Promise.resolve(dashboard1.toJSON()))));
      ViewManagementActions.update = mockAction(jest.fn((view) => Promise.resolve(view)));
      SearchActions.get = mockAction(jest.fn(() => Promise.resolve(searchDB1.toJSON())));
      SearchActions.create = mockAction(jest.fn(() => Promise.resolve({ search: searchDB1 })));

      asMock(CopyWidgetToDashboard).mockImplementation(() => View.builder()
        .search(Search.builder().id('search-id').build())
        .id('new-id').type(View.Type.Dashboard)
        .build());
    });

    const renderAndClick = () => {
      render(<DummyWidget />);
      const actionToggle = screen.getByTestId('widgetActionDropDown');

      fireEvent.click(actionToggle);
      const copyToDashboard = screen.getByText('Copy to Dashboard');

      fireEvent.click(copyToDashboard);
      const view1ListItem = screen.getByText('view 1');

      fireEvent.click(view1ListItem);
      const selectBtn = screen.getByText('Select');

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
