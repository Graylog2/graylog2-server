import Reflux from 'reflux';
import Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import ViewsActions from 'enterprise/actions/ViewsActions';
import CurrentViewActions from 'enterprise/actions/CurrentViewActions';

import { createEmptyQuery } from 'enterprise/logic/Queries';

export default Reflux.createStore({
  listenables: [ViewsActions],
  views: new Immutable.Map(),

  init() {
    const id = ObjectID();
    this.views = this.views.set(id, new Immutable.Map({ id: id, positions: {} }));
    CurrentViewActions.selectView(id);
    const defaultQuery = createEmptyQuery(id);
    CurrentViewActions.selectQuery(defaultQuery.id);
  },

  getInitialState() {
    return this.views;
  },

  create(view) {
    if (view.id === undefined) {
      throw new Error('Unable to add view without id.');
    }
    this.views = this.views.updateIn([view.id], (value) => {
      if (value) {
        throw new Error(`Unable to add view with id <${view.id}>, it is already present.`);
      }
      return new Immutable.Map(view);
    });
    this._trigger();
  },
  load(viewId, view) {
    this.views = this.views.set(viewId, view);
    this._trigger();
  },
  remove(viewId) {
    this.views = this.views.remove(viewId);
    this._trigger();
  },
  title(viewId, queryId, title) {
    this.views = this.views.updateIn([viewId, 'titles'], (titles) => {
      const titlesMap = titles || new Immutable.Map();
      return titlesMap.set(queryId, title);
    });
    this._trigger();
  },
  update(viewId, view) {
    this.views = this.views.set(viewId, new Immutable.Map(view));
    this._trigger();
  },
  _trigger() {
    this.trigger(this.views);
  },
});