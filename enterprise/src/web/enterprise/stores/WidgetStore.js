import Reflux from 'reflux';
import Immutable from 'immutable';
import uuid from 'uuid/v4';

import WidgetActions from 'enterprise/actions/WidgetActions';
import CurrentViewStore from './CurrentViewStore';

export default Reflux.createStore({
  listenables: [WidgetActions],

  widgets: new Immutable.Map(),
  selectedView: undefined,

  init() {
    this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  getInitialState() {
    if (this.selectedView) {
      return this.widgets.get(this.selectedView);
    }
    return new Immutable.Map();
  },

  onCurrentViewStoreChange(state) {
    if (this.selectedView !== state.selectedView) {
      this.selectedView = state.selectedView;
      this._trigger();
    }
  },

  create(viewId, queryId, widget) {
    if (widget.id === undefined) {
      throw new Error('Unable to add widget without id to query.');
    }
    this.widgets = this.widgets.setIn([viewId, queryId, widget.id], new Immutable.Map(widget));
    this._trigger();
  },
  duplicate(viewId, queryId, widgetId) {
    const widget = this.widgets.getIn([viewId, queryId, widgetId]);
    if (!widget) {
      throw new Error(`Unable to duplicate widget with id "${widgetId}", it is not found.`);
    }
    const duplicatedWidget = widget.set('id', uuid());
    this.widgets = this.widgets.setIn([viewId, queryId, duplicatedWidget.get('id')], duplicatedWidget);
    this._trigger();
    return duplicatedWidget;
  },
  remove(viewId, queryId, widgetId) {
    this.widgets = this.widgets.removeIn([viewId, queryId, widgetId]);
    this._trigger();
  },
  update(viewId, queryId, widgetId, widget) {
    this.widgets = this.widgets.setIn([viewId, queryId, widgetId], widget);
    this._trigger();
  },
  updateConfig(viewId, queryId, widgetId, config) {
    this.widgets = this.widgets.setIn([viewId, queryId, widgetId, 'config'], config);
    this._trigger();
  },
  _trigger() {
    if (this.selectedView) {
      this.trigger(this.widgets.get(this.selectedView, new Immutable.Map()));
    } else {
      this.trigger(new Immutable.Map());
    }
  },
});
