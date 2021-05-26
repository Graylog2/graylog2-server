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
import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';
import MockStore from 'helpers/mocking/StoreMock';
import asMock from 'helpers/mocking/AsMock';

import WidgetModel from 'views/logic/widgets/Widget';
import { WidgetActions, Widgets } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import View from 'views/logic/views/View';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import { TitlesMap } from 'views/stores/TitleTypes';

import Widget, { Result } from './Widget';

import WidgetContext from '../contexts/WidgetContext';
import WidgetFocusContext, { WidgetFocusContextType } from '../contexts/WidgetFocusContext';

jest.mock('../searchbar/QueryInput', () => mockComponent('QueryInput'));
jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('./WidgetColorContext', () => ({ children }) => children);

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: MockStore(),
  WidgetActions: {
    update: mockAction(),
  },
}));

const pluginManifest: PluginRegistration = {
  exports: {
    enterpriseWidgets: [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        visualizationComponent: () => <>dummy-visualization</>,
        // eslint-disable-next-line react/prop-types
        editComponent: ({ onChange }) => {
          // eslint-disable-next-line react/button-has-type
          return <button type="button" onClick={() => onChange({ foo: 23 })}>Click me</button>;
        },
        needsControlledHeight: () => true,
        searchTypes: () => [],
      },
      {
        type: 'default',
        visualizationComponent: () => <span>Unknown widget</span>,
        editComponent: () => <span>Unknown widget in edit mode</span>,
        needsControlledHeight: () => true,
        searchTypes: () => [],
      },
    ],
  },
};

describe('<Widget />', () => {
  beforeAll(() => {
    PluginStore.register(pluginManifest);
  });

  afterAll(() => {
    PluginStore.unregister(pluginManifest);
  });

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

  beforeEach(() => {
    ViewStore.getInitialState = jest.fn(() => viewStoreState);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
  });

  type DummyWidgetProps = {
    widget?: WidgetModel,
    errors?: Array<{ description: string }>,
    data?: { [key: string]: Result },
    focusedWidget?: WidgetFocusContextType['focusedWidget'],
    setWidgetFocusing?: WidgetFocusContextType['setWidgetFocusing'],
    setWidgetEditing?: WidgetFocusContextType['setWidgetEditing'],
    unsetWidgetFocusing?: WidgetFocusContextType['unsetWidgetFocusing'],
    unsetWidgetEditing?: WidgetFocusContextType['unsetWidgetEditing'],
    title?: string,
    editing?: boolean,
  }

  const DummyWidget = ({
    widget: propsWidget = widget,
    focusedWidget = undefined,
    setWidgetFocusing = () => {},
    setWidgetEditing = () => {},
    unsetWidgetFocusing = () => {},
    unsetWidgetEditing = () => {},
    ...props
  }: DummyWidgetProps) => (
    <WidgetFocusContext.Provider value={{ focusedWidget, setWidgetFocusing, setWidgetEditing, unsetWidgetFocusing, unsetWidgetEditing }}>
      <WidgetContext.Provider value={propsWidget}>
        <Widget widget={propsWidget}
                id="widgetId"
                fields={Immutable.List([])}
                onPositionsChange={() => {}}
                onSizeChange={() => {}}
                title="Widget Title"
                position={new WidgetPosition(1, 1, 1, 1)}
                {...props} />
      </WidgetContext.Provider>
    </WidgetFocusContext.Provider>
  );

  it('should render with empty props', () => {
    render(<DummyWidget />);

    expect(screen.getByTitle('Widget Title')).toBeInTheDocument();
  });

  it('should render loading widget for widget without data', () => {
    render(<DummyWidget />);

    expect(screen.queryAllByTestId('loading-widget')).toHaveLength(1);
  });

  it('should render error widget for widget with one error', () => {
    render(<DummyWidget errors={[{ description: 'The widget has failed: the dungeon collapsed, you die!' }]} />);
    const errorWidgets = screen.queryAllByText('The widget has failed: the dungeon collapsed, you die!');

    expect(errorWidgets).toHaveLength(1);
  });

  it('should render error widget including all error messages for widget with multiple errors', () => {
    render((
      <DummyWidget errors={[
        { description: 'Something is wrong' },
        { description: 'Very wrong' },
      ]} />
    ));

    const errorWidgets1 = screen.queryAllByText('Something is wrong');

    expect(errorWidgets1).toHaveLength(1);

    const errorWidgets2 = screen.queryAllByText('Very wrong');

    expect(errorWidgets2).toHaveLength(1);
  });

  it('should render correct widget visualization for widget with data', () => {
    render(<DummyWidget data={{}} />);

    expect(screen.queryAllByTestId('loading-widget')).toHaveLength(0);
    expect(screen.queryAllByTitle('Widget Title')).toHaveLength(1);
  });

  it('renders placeholder if widget type is unknown', async () => {
    const unknownWidget = WidgetModel.builder()
      .id('widgetId')
      .type('i-dont-know-this-widget-type')
      .config({})
      .build();
    const UnknownWidget = (props) => (
      <DummyWidget widget={unknownWidget}
                   id="widgetId"
                   fields={[]}
                   onPositionsChange={() => {}}
                   onSizeChange={() => {}}
                   title="Widget Title"
                   position={new WidgetPosition(1, 1, 1, 1)}
                   {...props} />

    );

    render(
      <UnknownWidget data={[]} />,
    );

    await screen.findByText('Unknown widget');
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

    render(
      <UnknownWidget data={[]} />,
    );

    await screen.findByText('Unknown widget in edit mode');
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

  it('adds cancel action to widget in edit mode', () => {
    render(<DummyWidget editing />);
    const cancel = screen.queryAllByText('Cancel');

    expect(cancel).toHaveLength(1);
  });

  it('updates focus mode, on widget edit cancel', () => {
    const mockUnsetWidgetEditing = jest.fn();
    render(<DummyWidget editing unsetWidgetEditing={mockUnsetWidgetEditing} />);
    const cancel = screen.getByText('Cancel');
    fireEvent.click(cancel);

    expect(mockUnsetWidgetEditing).toHaveBeenCalledTimes(1);
  });

  it('updates focus mode, on widget edit save', () => {
    const mockUnsetWidgetEditing = jest.fn();
    render(<DummyWidget editing unsetWidgetEditing={mockUnsetWidgetEditing} />);
    const saveButton = screen.getByText('Save');
    fireEvent.click(saveButton);

    expect(mockUnsetWidgetEditing).toHaveBeenCalledTimes(1);
  });

  it('does not trigger action when clicking cancel after no changes were made', () => {
    render(<DummyWidget editing />);

    WidgetActions.updateConfig = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));

    const cancelBtn = screen.getByText('Cancel');

    fireEvent.click(cancelBtn);

    expect(WidgetActions.updateConfig).not.toHaveBeenCalled();
  });

  it('restores original state of widget config when clicking cancel after changes were made', () => {
    const widgetWithConfig = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({ foo: 42 })
      .build();
    render(<DummyWidget editing widget={widgetWithConfig} />);

    WidgetActions.updateConfig = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    WidgetActions.update = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    const onChangeBtn = screen.getByText('Click me');

    fireEvent.click(onChangeBtn);

    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = screen.getByText('Cancel');

    fireEvent.click(cancelButton);

    expect(WidgetActions.update).toHaveBeenCalledWith('widgetId', widgetWithConfig);
  });

  it('does not restore original state of widget config when clicking "Finish Editing"', () => {
    const widgetWithConfig = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({ foo: 42 })
      .build();
    render(<DummyWidget editing widget={widgetWithConfig} />);

    WidgetActions.updateConfig = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    WidgetActions.update = mockAction(jest.fn(async () => Immutable.OrderedMap() as Widgets));
    const onChangeBtn = screen.getByText('Click me');

    fireEvent.click(onChangeBtn);

    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const saveButton = screen.getByText('Save');

    fireEvent.click(saveButton);

    expect(WidgetActions.update).not.toHaveBeenCalledWith('widgetId', { config: { foo: 42 }, id: 'widgetId', type: 'dummy' });
  });
});
