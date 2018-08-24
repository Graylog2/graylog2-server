import Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { CurrentViewStateStore } from './CurrentViewStateStore';
import ViewState from '../logic/views/ViewState';
import { ViewStatesActions } from './ViewStatesStore';
import WidgetPosition from '../logic/widgets/WidgetPosition';

describe('CurrentViewStateStore', () => {
  const viewState = ViewState.create();
  const viewId = 'beef-1000';
  const viewMap = {};
  viewMap[viewId] = viewState;
  const statesMap = Immutable.Map(viewMap);
  PluginStore.exports = () => {
    return [{ type: 'MESSAGES', defaultHeight: 5, defaultWidth: 6 }];
  };

  it('should set empty widgets', () => {
    const updateFn = jest.fn((id, view) => {
      expect(id).toEqual(viewId);
      expect(view).toEqual(viewState);
    });
    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: viewState });
    CurrentViewStateStore.onViewStatesStoreChange(statesMap);
    CurrentViewStateStore.widgets([]);

    expect(updateFn.mock.calls.length).toBe(1);
  });

  it('should set new widgets', () => {
    const widgetPos = new WidgetPosition(1, 1, 5, 6);
    const widgetPositionsMap = { dead: widgetPos };
    const widgets = [
      { id: 'dead', type: 'MESSAGES' },
    ];
    const newView = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(widgets)
      .build();

    const updateFn = jest.fn((id, view) => {
      expect(id).toEqual(viewId);
      expect(view).toEqual(newView);
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
    const oldWidget = { id: 'dead', type: 'MESSAGES' };
    const oldWidgets = [oldWidget];
    const oldView = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(oldWidgets)
      .build();

    viewMap[viewId] = oldView;
    const sMap = Immutable.Map(viewMap);

    const newWidget = { id: 'feed', type: 'MESSAGES' };
    const expectedWidgets = [oldWidget, newWidget];
    const newView = viewState.toBuilder()
      .widgetPositions(widgetPositionsMap)
      .widgets(expectedWidgets)
      .build();

    const updateFn = jest.fn((id, view) => {
      expect(id).toEqual(viewId);
      expect(view).toEqual(newView);
    });

    ViewStatesActions.update = updateFn;
    CurrentViewStateStore.onViewStoreChange({ activeQuery: viewId, view: oldView });
    CurrentViewStateStore.onViewStatesStoreChange(sMap);
    CurrentViewStateStore.widgets(expectedWidgets);
    expect(updateFn.mock.calls.length).toBe(1);
  });
});
