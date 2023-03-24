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
import type { PluginRegistration } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';

import mockComponent from 'helpers/mocking/MockComponent';
import asMock from 'helpers/mocking/AsMock';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import useWidgetResults from 'views/components/useWidgetResults';
import type SearchError from 'views/logic/SearchError';
import { viewSliceReducer } from 'views/logic/slices/viewSlice';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { searchExecutionSliceReducer } from 'views/logic/slices/searchExecutionSlice';
import { duplicateWidget, updateWidgetConfig, updateWidget } from 'views/logic/slices/widgetActions';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import Widget from './Widget';
import type { Props as WidgetComponentProps } from './Widget';

import WidgetContext from '../contexts/WidgetContext';
import type { WidgetFocusContextType } from '../contexts/WidgetFocusContext';
import WidgetFocusContext from '../contexts/WidgetFocusContext';
import FieldTypesContext from '../contexts/FieldTypesContext';

jest.mock('../searchbar/queryinput/QueryInput', () => mockComponent('QueryInput'));
jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('./WidgetColorContext', () => ({ children }) => children);
jest.mock('views/components/useWidgetResults');

jest.mock('views/logic/slices/widgetActions', () => ({
  ...jest.requireActual('views/logic/slices/widgetActions'),
  duplicateWidget: jest.fn(() => async () => {}),
  updateWidget: jest.fn(() => async () => {}),
  updateWidgetConfig: jest.fn(() => async () => {}),
}));

const pluginManifest: PluginRegistration = {
  exports: {
    'views.reducers': [
      { key: 'view', reducer: viewSliceReducer },
      { key: 'searchExecution', reducer: searchExecutionSliceReducer },
    ],
    enterpriseWidgets: [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        visualizationComponent: () => <>dummy-visualization</>,

        editComponent: ({ onChange }) => {
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
  beforeAll(() => PluginStore.register(pluginManifest));

  afterAll(() => PluginStore.unregister(pluginManifest));

  const widget = WidgetModel.builder().newId()
    .type('dummy')
    .config({ queryId: 'query-id-1' })
    .build();

  const fieldTypes = {
    all: Immutable.List<FieldTypeMapping>(),
    queryFields: Immutable.Map<string, Immutable.List<FieldTypeMapping>>(),
  };

  type DummyWidgetProps = Partial<WidgetComponentProps> & {
    focusedWidget?: WidgetFocusContextType['focusedWidget'],
    setWidgetFocusing?: WidgetFocusContextType['setWidgetFocusing'],
    setWidgetEditing?: WidgetFocusContextType['setWidgetEditing'],
    unsetWidgetFocusing?: WidgetFocusContextType['unsetWidgetFocusing'],
    unsetWidgetEditing?: WidgetFocusContextType['unsetWidgetEditing'],
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
    <TestStoreProvider>
      <FieldTypesContext.Provider value={fieldTypes}>
        {}
        <WidgetFocusContext.Provider value={{ focusedWidget, setWidgetFocusing, setWidgetEditing, unsetWidgetFocusing, unsetWidgetEditing }}>
          <WidgetContext.Provider value={propsWidget}>
            <Widget widget={propsWidget}
                    id="widgetId"
                    onPositionsChange={() => {}}
                    title="Widget Title"
                    position={new WidgetPosition(1, 1, 1, 1)}
                    {...props} />
          </WidgetContext.Provider>
        </WidgetFocusContext.Provider>
      </FieldTypesContext.Provider>
    </TestStoreProvider>
  );

  const getWidgetUpdateButton = () => screen.getByRole('button', { name: /update widget/i });

  it('should render with empty props', async () => {
    asMock(useWidgetResults).mockReturnValue({ widgetData: undefined, error: undefined });
    render(<DummyWidget />);

    await screen.findByTitle('Widget Title');
  });

  it('should render loading widget for widget without data', async () => {
    asMock(useWidgetResults).mockReturnValue({ widgetData: undefined, error: undefined });
    render(<DummyWidget />);

    await screen.findByTestId('loading-widget');
  });

  it('should render error widget for widget with one error', async () => {
    asMock(useWidgetResults).mockReturnValue({ error: [{ description: 'The widget has failed: the dungeon collapsed, you die!' } as SearchError], widgetData: undefined });
    render(<DummyWidget />);

    await screen.findByText('The widget has failed: the dungeon collapsed, you die!');
  });

  it('should render error widget including all error messages for widget with multiple errors', async () => {
    asMock(useWidgetResults).mockReturnValue({
      error: [
        { description: 'Something is wrong' } as SearchError,
        { description: 'Very wrong' } as SearchError,
      ],
      widgetData: undefined,
    });

    render(<DummyWidget />);

    await screen.findByText('Something is wrong');
    await screen.findByText('Very wrong');
  });

  it('should render correct widget visualization for widget with data', async () => {
    asMock(useWidgetResults).mockReturnValue({ widgetData: {}, error: [] });
    render(<DummyWidget />);

    await screen.findByTitle('Widget Title');

    expect(screen.queryAllByTestId('loading-widget')).toHaveLength(0);
  });

  it('renders placeholder if widget type is unknown', async () => {
    asMock(useWidgetResults).mockReturnValue({ widgetData: {}, error: [] });
    const unknownWidget = WidgetModel.builder()
      .id('widgetId')
      .type('i-dont-know-this-widget-type')
      .config({})
      .build();
    const UnknownWidget = (props) => (
      <DummyWidget widget={unknownWidget}
                   id="widgetId"
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
    const UnknownWidget = (props: Partial<React.ComponentProps<typeof Widget>>) => (
      <FieldTypesContext.Provider value={fieldTypes}>
        <TestStoreProvider>
          <WidgetContext.Provider value={unknownWidget}>
            <Widget widget={unknownWidget}
                    editing
                    id="widgetId"
                    onPositionsChange={() => {}}
                    title="Widget Title"
                    position={new WidgetPosition(1, 1, 1, 1)}
                    {...props} />
          </WidgetContext.Provider>
        </TestStoreProvider>
      </FieldTypesContext.Provider>
    );

    render(
      <UnknownWidget />,
    );

    await screen.findByText('Unknown widget in edit mode');
  });

  it('copies title when duplicating widget', async () => {
    render(<DummyWidget title="Dummy Widget" />);

    const actionToggle = screen.getByTestId('widgetActionDropDown');

    fireEvent.click(actionToggle);
    const duplicateBtn = screen.getByText('Duplicate');

    fireEvent.click(duplicateBtn);

    await waitFor(() => expect(duplicateWidget).toHaveBeenCalled());
  });

  it('adds cancel action to widget in edit mode', async () => {
    render(<DummyWidget editing />);
    await screen.findByText('Cancel');
  });

  it('updates focus mode, on widget edit cancel', async () => {
    const mockUnsetWidgetEditing = jest.fn();
    render(<DummyWidget editing unsetWidgetEditing={mockUnsetWidgetEditing} />);
    const cancel = await screen.findByText('Cancel');
    fireEvent.click(cancel);

    await waitFor(() => { expect(mockUnsetWidgetEditing).toHaveBeenCalledTimes(1); });
  });

  it('updates focus mode, on widget edit save', async () => {
    const mockUnsetWidgetEditing = jest.fn();
    render(<DummyWidget editing unsetWidgetEditing={mockUnsetWidgetEditing} />);
    const updateWidgetButton = getWidgetUpdateButton();
    fireEvent.click(updateWidgetButton);

    await waitFor(() => expect(updateWidgetButton).not.toBeDisabled());

    expect(mockUnsetWidgetEditing).toHaveBeenCalledTimes(1);
  });

  it('does not trigger action when clicking cancel after no changes were made', () => {
    render(<DummyWidget editing />);

    const cancelBtn = screen.getByText('Cancel');

    fireEvent.click(cancelBtn);

    expect(updateWidgetConfig).not.toHaveBeenCalled();
  });

  it('restores original state of widget config when clicking cancel after changes were made', () => {
    const widgetWithConfig = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({ foo: 42 })
      .build();
    render(<DummyWidget editing widget={widgetWithConfig} />);

    const onChangeBtn = screen.getByText('Click me');

    fireEvent.click(onChangeBtn);

    expect(updateWidgetConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = screen.getByText('Cancel');

    fireEvent.click(cancelButton);

    expect(updateWidget).toHaveBeenCalledWith('widgetId', widgetWithConfig);
  });

  it('does not restore original state of widget config when clicking "Update widget"', async () => {
    const widgetWithConfig = WidgetModel.builder()
      .id('widgetId')
      .type('dummy')
      .config({ foo: 42 })
      .build();
    render(<DummyWidget editing widget={widgetWithConfig} />);

    const onChangeBtn = screen.getByText('Click me');

    fireEvent.click(onChangeBtn);

    expect(updateWidgetConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const updateWidgetButton = getWidgetUpdateButton();
    fireEvent.click(updateWidgetButton);

    await waitFor(() => expect(updateWidgetButton).not.toBeDisabled());

    expect(updateWidget).not.toHaveBeenCalledWith('widgetId', { config: { foo: 42 }, id: 'widgetId', type: 'dummy' });
  });
});
