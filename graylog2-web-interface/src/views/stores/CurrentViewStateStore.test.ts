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
// @flow strict
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';
import mockAction from 'helpers/mocking/MockAction';

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
    const updateFn = mockAction(jest.fn(() => Promise.resolve(viewState)));

    ViewStatesActions.update = updateFn;

    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    await CurrentViewStateStore.widgets(Immutable.List());

    expect(updateFn).toHaveBeenCalledTimes(1);
    expect(updateFn).toHaveBeenCalledWith(viewId, viewState);
  });

  it('should set new widgets', async () => {
    const oldWidgetId = 'dead';
    const expectedViewState = viewState.toBuilder()
      .widgetPositions({ [oldWidgetId]: new WidgetPosition(1, 1, 5, 6) })
      .widgets([MessagesWidget.builder().id(oldWidgetId).build()])
      .build();
    const updateFn = mockAction(jest.fn(() => Promise.resolve(expectedViewState)));

    ViewStatesActions.update = updateFn;

    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    await CurrentViewStateStore.widgets(expectedViewState.widgets);

    expect(updateFn).toHaveBeenCalledTimes(1);
    expect(updateFn).toHaveBeenCalledWith(viewId, expectedViewState);
  });

  it('should add new widgets', async () => {
    const oldWidgetId = 'dead';
    const oldViewState = viewState.toBuilder()
      .widgetPositions({ [oldWidgetId]: new WidgetPosition(1, 1, 5, 6) })
      .widgets([MessagesWidget.builder().id(oldWidgetId).build()])
      .build();

    viewStateMap[viewId] = oldViewState;
    const sMap = Immutable.Map(viewStateMap);

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
    const expectedWidgets = [oldViewState.widgets.get(0), MessagesWidget.builder().id('feed').build()];
    const expectedWidgetPosition = { [oldWidgetId]: newWidgetPositionDead, feed: newWidgetPositionFeed };
    const expectedViewState = viewState.toBuilder()
      .widgetPositions(expectedWidgetPosition)
      .widgets(expectedWidgets)
      .build();

    const updateFn = mockAction(jest.fn(() => Promise.resolve(expectedViewState)));

    ViewStatesActions.update = updateFn;

    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: oldViewState });
    CurrentViewStateStore.onViewStatesStoreChange(sMap);
    await CurrentViewStateStore.widgets(expectedWidgets);

    expect(updateFn).toHaveBeenCalledTimes(1);
    expect(updateFn).toHaveBeenCalledWith(viewId, expectedViewState);
  });

  it('should remove widget positions for deleted widgets', async () => {
    const widgetOne = MessagesWidget.builder().id('widget-one').build();
    const widgetOnePos = new WidgetPosition(1, 1, 5, 6);
    const existingViewState = viewState.toBuilder()
      .widgetPositions({
        'widget-one': widgetOnePos,
        'widget-two': new WidgetPosition(1, 6, 5, 6),
      })
      .widgets([
        widgetOne,
        MessagesWidget.builder().id('widget-two').build(),
      ])
      .build();

    viewStateMap[viewId] = existingViewState;
    const sMap = Immutable.Map(viewStateMap);

    const expectedWidgets = [widgetOne];
    const expectedViewState = viewState.toBuilder()
      .widgetPositions({ 'widget-one': widgetOnePos })
      .widgets(expectedWidgets)
      .build();
    const updateFn = mockAction(jest.fn(() => Promise.resolve(expectedViewState)));

    ViewStatesActions.update = updateFn;

    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: existingViewState });
    CurrentViewStateStore.onViewStatesStoreChange(sMap);
    await CurrentViewStateStore.widgets(expectedWidgets);

    expect(updateFn).toHaveBeenCalledTimes(1);
    expect(updateFn).toHaveBeenCalledWith(viewId, expectedViewState);
  });
});
