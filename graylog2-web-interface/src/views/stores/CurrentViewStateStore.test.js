// @flow strict
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

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

  it('should set empty widgets', () => {
    const updateFn = jest.fn((id, view) => {
      expect(id).toEqual(viewId);
      expect(view).toEqual(viewState);
      return Promise.resolve(viewState);
    });
    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    CurrentViewStateStore.widgets(Immutable.List());

    expect(updateFn.mock.calls.length).toBe(1);
  });

  it('should set new widgets', () => {
    const widgetPos = new WidgetPosition(1, 1, 5, 6);
    const widgetPositionsMap = { dead: widgetPos };
    const widgets = [
      MessagesWidget.builder().id('dead').build(),
    ];
    const expectedViewState = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(widgets)
      .build();

    const updateFn = jest.fn((id, newViewState) => {
      expect(id).toEqual(viewId);
      expect(newViewState).toEqual(expectedViewState);
      return Promise.resolve(expectedViewState);
    });

    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    CurrentViewStateStore.widgets(widgets);
    expect(updateFn.mock.calls.length).toBe(1);
  });

  it('should add new widgets', () => {
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

    const updateFn = jest.fn((id, newViewState) => {
      expect(id).toEqual(viewId);
      expect(newViewState).toEqual(expectedViewState);
      return Promise.resolve(expectedViewState);
    });

    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: oldViewState });
    CurrentViewStateStore.onViewStatesStoreChange(sMap);
    CurrentViewStateStore.widgets(expectedWidgets);
    expect(updateFn.mock.calls.length).toBe(1);
  });
});
