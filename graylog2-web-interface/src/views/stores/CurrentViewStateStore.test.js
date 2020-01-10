// @flow strict
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import ViewState from 'views/logic/views/ViewState';
import { CurrentViewStateStore } from './CurrentViewStateStore';
import { ViewStatesActions } from './ViewStatesStore';

describe('CurrentViewStateStore', () => {
  const viewState = ViewState.create();
  const viewId = 'beef-1000';
  const viewStateMap = {};
  viewStateMap[viewId] = viewState;
  const statesMap = Immutable.Map(viewStateMap);
  PluginStore.exports = () => {
    return [{ type: 'MESSAGES', defaultHeight: 5, defaultWidth: 6 }];
  };

  it('should set empty widgets', async () => {
    const updateFn = mockAction(jest.fn((id, view) => {
      expect(id).toEqual(viewId);
      expect(view).toEqual(viewState);
      return Promise.resolve(viewState);
    }));
    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    await CurrentViewStateStore.widgets(Immutable.List());

    expect(asMock(updateFn).mock.calls.length).toBe(1);
  });

  it('should set new widgets', async () => {
    const widgetPos = new WidgetPosition(1, 1, 5, 6);
    const widgetPositionsMap = { dead: widgetPos };
    const widgets = [
      MessagesWidget.builder().id('dead').build(),
    ];
    const expectedViewState = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(widgets)
      .build();

    const updateFn = mockAction(jest.fn((id, newViewState) => {
      expect(id).toEqual(viewId);
      expect(newViewState).toEqual(expectedViewState);
      return Promise.resolve(expectedViewState);
    }));

    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    await CurrentViewStateStore.widgets(widgets);
    expect(updateFn).toHaveBeenCalledTimes(1);
  });

  it('should add new widgets', async () => {
    const widgetPos = new WidgetPosition(1, 1, 5, 6);
    const widgetPositionsMap = { dead: widgetPos };
    const oldWidget = MessagesWidget.builder().id('dead').build();
    const oldWidgets = [oldWidget];
    const oldViewState = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(oldWidgets)
      .build();

    viewStateMap[viewId] = oldViewState;
    const sMap = Immutable.Map(viewStateMap);

    const newWidget = MessagesWidget.builder().id('feed').build();
    const newWidgetPositionDead = WidgetPosition.builder()
      .col(1)
      .row(6)
      .height(5)
      .width(6)
      .build();
    const newWidgetPositionFeed = WidgetPosition.builder()
      .col(1)
      .row(1)
      .height(5)
      .width(6)
      .build();
    const expectedWidgets = [oldWidget, newWidget];
    const expectedWidgetPosition = { dead: newWidgetPositionDead, feed: newWidgetPositionFeed };
    const expectedViewState = viewState.toBuilder()
      .widgetPositions(expectedWidgetPosition)
      .widgets(expectedWidgets)
      .build();

    const updateFn = mockAction(jest.fn((id, newViewState) => {
      expect(id).toEqual(viewId);
      expect(newViewState).toEqual(expectedViewState);
      return Promise.resolve(expectedViewState);
    }));

    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: oldViewState });
    CurrentViewStateStore.onViewStatesStoreChange(sMap);
    await CurrentViewStateStore.widgets(expectedWidgets);
    expect(updateFn).toHaveBeenCalledTimes(1);
  });

  it('should remove widget positions for deleted widgets', async () => {
    const widgetOnePos = new WidgetPosition(1, 1, 5, 6);
    const widgetTwoPos = new WidgetPosition(1, 6, 5, 6);
    const widgetPositionsMap = { 'widget-one': widgetOnePos, 'widget-two': widgetTwoPos };
    const widgetOne = MessagesWidget.builder().id('widget-one').build();
    const widgetTwo = MessagesWidget.builder().id('widget-two').build();
    const existingWidgets = [widgetOne, widgetTwo];
    const existingViewState = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(existingWidgets)
      .build();

    viewStateMap[viewId] = existingViewState;
    const sMap = Immutable.Map(viewStateMap);

    const expectedWidgets = [widgetOne];
    const expectedWidgetPosition = { 'widget-one': widgetOnePos };
    const expectedViewState = viewState.toBuilder()
      .widgetPositions(expectedWidgetPosition)
      .widgets(expectedWidgets)
      .build();

    const updateFn = mockAction(jest.fn((id, newViewState) => {
      expect(id).toEqual(viewId);
      expect(newViewState).toEqual(expectedViewState);
      return Promise.resolve(expectedViewState);
    }));

    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: existingViewState });
    CurrentViewStateStore.onViewStatesStoreChange(sMap);
    await CurrentViewStateStore.widgets(expectedWidgets);
    expect(updateFn).toHaveBeenCalledTimes(1);
  });
});
