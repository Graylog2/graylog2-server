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
import { PluginStore } from 'graylog-web-plugin/plugin';

import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';
import { MockStore } from 'helpers/mocking';
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
import type { TitlesMap } from 'views/stores/TitleTypes';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { ViewStore } from 'views/stores/ViewStore';

import WidgetActionsMenu from './WidgetActionsMenu';

import WidgetContext from '../contexts/WidgetContext';
import type { WidgetFocusContextType } from '../contexts/WidgetFocusContext';
import WidgetFocusContext from '../contexts/WidgetFocusContext';

jest.mock('views/components/search/IfSearch', () => jest.fn(({ children }) => children));

jest.mock('views/logic/views/CopyWidgetToDashboard', () => jest.fn());

jest.mock('views/stores/ChartColorRulesStore', () => ({
  ChartColorRulesStore: {},
}));

jest.mock('views/logic/views/Actions');

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: MockStore(),
  ViewActions: {
    create: mockAction(),
    load: mockAction(),
  },
}));

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    remove: jest.fn(),
  },
}));

jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: MockStore(),
}));

jest.mock('views/stores/CurrentViewStateStore', () => ({ CurrentViewStateStore: MockStore(['getInitialState', () => ({})]) }));

const openActionDropdown = async () => {
  const actionToggle = await screen.findByTestId('widgetActionDropDown');

  fireEvent.click(actionToggle);
  await screen.findByRole('heading', { name: 'Actions' });
};

describe('<WidgetActionsMenu />', () => {
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

  asMock(ViewStore.getInitialState).mockReturnValue(viewStoreState);

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
    unsetWidgetFocusing?: WidgetFocusContextType['unsetWidgetFocusing'],
    unsetWidgetEditing?: WidgetFocusContextType['unsetWidgetEditing'],
    title?: string
    isFocused?: boolean,
  }

  const DummyWidget = ({
    widget: propsWidget = widget,
    setWidgetFocusing = () => {},
    setWidgetEditing = () => {},
    unsetWidgetFocusing = () => {},
    unsetWidgetEditing = () => {},
    focusedWidget,
    ...props
  }: DummyWidgetProps) => (
    <FieldTypesContext.Provider value={{ all: Immutable.List(), queryFields: Immutable.Map() }}>
      <WidgetFocusContext.Provider value={{
        setWidgetFocusing,
        setWidgetEditing,
        unsetWidgetFocusing,
        unsetWidgetEditing,
        focusedWidget,
      }}>
        <WidgetContext.Provider value={propsWidget}>
          <WidgetActionsMenu isFocused={false}
                             toggleEdit={() => {}}
                             title="Widget Title"
                             position={new WidgetPosition(1, 1, 1, 1)}
                             onPositionsChange={() => {}}
                             {...props} />
        </WidgetContext.Provider>
      </WidgetFocusContext.Provider>
    </FieldTypesContext.Provider>
  );

  it('is updating widget focus context on focus', () => {
    const mockSetWidgetFocusing = jest.fn();
    render(<DummyWidget title="Dummy Widget" setWidgetFocusing={mockSetWidgetFocusing} />);

    const focusButton = screen.getByTitle('Focus this widget');

    fireEvent.click(focusButton);

    expect(mockSetWidgetFocusing).toHaveBeenCalledWith('widget-id');
  });

  it('is updating widget focus context on un-focus', () => {
    const mockUnsetWidgetFocusing = jest.fn();
    render(<DummyWidget title="Dummy Widget" isFocused unsetWidgetFocusing={mockUnsetWidgetFocusing} />);

    const unfocusButton = screen.getByTitle('Un-focus widget');

    fireEvent.click(unfocusButton);

    expect(mockUnsetWidgetFocusing).toHaveBeenCalledTimes(1);
  });

  it('copies title when duplicating widget', async () => {
    render(<DummyWidget title="Dummy Widget" />);

    await openActionDropdown();

    const duplicateBtn = screen.getByText('Duplicate');

    WidgetActions.duplicate = mockAction(jest.fn().mockResolvedValue(WidgetModel.builder().id('duplicatedWidgetId').build()));

    TitlesActions.set = mockAction(jest.fn().mockResolvedValue(Immutable.Map() as TitlesMap));

    fireEvent.click(duplicateBtn);

    await waitFor(() => expect(WidgetActions.duplicate).toHaveBeenCalled());
    await waitFor(() => expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, 'duplicatedWidgetId', 'Dummy Widget (copy)'));
  });

  it('does not display export action if widget is not a message table', async () => {
    const dummyWidget = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({})
      .build();
    const { queryByText } = render(<DummyWidget title="Dummy Widget" widget={dummyWidget} />);

    await openActionDropdown();

    expect(queryByText('Export')).toBeNull();
  });

  it('allows export for message tables', async () => {
    const messagesWidget = MessagesWidget.builder()
      .id('widgetId')
      .config({})
      .build();

    render(<DummyWidget title="Dummy Widget" widget={messagesWidget} />);

    await openActionDropdown();

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

    const renderAndClick = async () => {
      render(<DummyWidget />);

      await openActionDropdown();

      const copyToDashboard = screen.getByText('Copy to Dashboard');

      fireEvent.click(copyToDashboard);
      const view1ListItem = screen.getByText('view 1');

      fireEvent.click(view1ListItem);
      const selectBtn = screen.getByText('Select');

      fireEvent.click(selectBtn);
    };

    it('should get dashboard from backend', async () => {
      await renderAndClick();
      await waitFor(() => expect(ViewManagementActions.get).toHaveBeenCalledTimes(1));

      expect(ViewManagementActions.get).toHaveBeenCalledWith('view-1');
    });

    it('should get corresponding search to dashboard', async () => {
      await renderAndClick();
      await waitFor(() => expect(SearchActions.get).toHaveBeenCalledTimes(1));

      expect(SearchActions.get).toHaveBeenCalledWith('search-1');
    });

    it('should create new search for dashboard', async () => {
      await renderAndClick();
      await waitFor(() => expect(SearchActions.create).toHaveBeenCalledTimes(1));

      expect(SearchActions.create).toHaveBeenCalledWith(Search.builder().id('search-id').parameters([]).queries([])
        .build());
    });

    it('should update dashboard with new search and widget', async () => {
      await renderAndClick();
      await waitFor(() => expect(ViewManagementActions.update).toHaveBeenCalledTimes(1));

      expect(ViewManagementActions.update).toHaveBeenCalledWith(
        View.builder()
          .search(Search.builder().id('search-1').build())
          .id('new-id').type(View.Type.Dashboard)
          .build(),
      );
    });

    it('should redirect to updated dashboard', async () => {
      await renderAndClick();
      await waitFor(() => expect(loadDashboard).toHaveBeenCalled());

      expect(loadDashboard).toHaveBeenCalledWith('view-1');
    });
  });

  describe('delete action', () => {
    let oldWindowConfirm;

    beforeEach(() => {
      asMock(WidgetActions.remove).mockClear();
      oldWindowConfirm = window.confirm;
      window.confirm = jest.fn();
    });

    afterEach(() => {
      window.confirm = oldWindowConfirm;
    });

    it('should delete widget when no deletion hook is installed and prompt is confirmed', async () => {
      asMock(window.confirm).mockReturnValue(true);

      render(<DummyWidget />);

      await openActionDropdown();

      fireEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));

      await waitFor(() => expect(WidgetActions.remove).toHaveBeenCalledWith('widget-id'));
    });

    it('should not delete widget when no deletion hook is installed and prompt is cancelled', async () => {
      asMock(window.confirm).mockReturnValue(false);

      render(<DummyWidget />);

      await openActionDropdown();

      fireEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));

      await waitFor(() => expect(window.confirm).toHaveBeenCalled());

      expect(WidgetActions.remove).not.toHaveBeenCalled();
    });

    describe('with custom deletion hook', () => {
      const deletingWidgetHook = jest.fn();
      const plugin = {
        exports: {
          'views.hooks.confirmDeletingWidget': [deletingWidgetHook],
        },
      };

      beforeEach(() => {
        PluginStore.register(plugin);
      });

      afterEach(() => {
        PluginStore.unregister(plugin);
        asMock(deletingWidgetHook).mockClear();
      });

      it('should delete widget when deletion hook is installed that returns true', async () => {
        asMock(deletingWidgetHook).mockResolvedValue(true);
        asMock(window.confirm).mockReturnValue(null);

        render(<DummyWidget />);

        await openActionDropdown();

        fireEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));

        await waitFor(() => expect(WidgetActions.remove).toHaveBeenCalledWith('widget-id'));

        expect(deletingWidgetHook).toHaveBeenCalled();
      });

      it('should not delete widget when deletion hook is installed that returns false', async () => {
        asMock(deletingWidgetHook).mockResolvedValue(false);
        asMock(window.confirm).mockReturnValue(null);

        render(<DummyWidget />);

        await openActionDropdown();

        fireEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));

        expect(WidgetActions.remove).not.toHaveBeenCalledWith('widget-id');

        expect(deletingWidgetHook).toHaveBeenCalled();
      });

      it('should skip custom deletion hook if it throws exception', async () => {
        const e = new Error('Boom!');
        asMock(deletingWidgetHook).mockRejectedValue(e);
        asMock(window.confirm).mockReturnValue(true);

        render(<DummyWidget />);

        await openActionDropdown();

        fireEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));

        /* eslint-disable no-console */
        const oldConsoleTrace = console.trace;
        console.trace = jest.fn();

        await waitFor(() => expect(WidgetActions.remove).toHaveBeenCalledWith('widget-id'));

        expect(console.trace).toHaveBeenCalledWith('Exception occurred in deletion confirmation hook: ', e);

        expect(deletingWidgetHook).toHaveBeenCalled();
        expect(window.confirm).toHaveBeenCalled();

        console.trace = oldConsoleTrace;
        /* eslint-enable no-console */
      });
    });
  });
});
