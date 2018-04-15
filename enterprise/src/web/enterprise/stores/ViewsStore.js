import Reflux from 'reflux';
import Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import ViewsActions from 'enterprise/actions/ViewsActions';
import CurrentViewActions from 'enterprise/actions/CurrentViewActions';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';

import { createEmptyQuery } from 'enterprise/logic/Queries';

export default Reflux.createStore({
  listenables: [ViewsActions],
  views: new Immutable.Map(),

  getInitialState() {
    return this.views;
  },

  create() {
    const id = ObjectID().toString();
    this.views = this.views.set(id, new Immutable.Map({ id: id, positions: {} }));
    const selectView = CurrentViewActions.selectView.triggerPromise(id);
    const defaultQuery = createEmptyQuery(id);
    const selectQuery = CurrentViewActions.selectQuery.triggerPromise(defaultQuery.id);

    ViewsActions.create.promise(Promise.all([selectView, selectQuery]));
    this._trigger();
  },
  load(viewId, view) {
    this.views = this.views.set(viewId, view);
    ViewsActions.load.promise(Promise.resolve(this.views));
    this._trigger();
  },
  remove(viewId) {
    this.views = this.views.remove(viewId);
    ViewsActions.remove.promise(Promise.resolve(this.views));
    this._trigger();
  },
  title(viewId, queryId, title) {
    this.views = this.views.updateIn([viewId, 'titles'], (titles) => {
      const titlesMap = titles || new Immutable.Map();
      return titlesMap.set(queryId, title);
    });
    ViewsActions.title.promise(Promise.resolve(this.views));
    this._trigger();
  },
  update(viewId, view) {
    this.views = this.views.set(viewId, new Immutable.Map(view));
    ViewsActions.update.promise(Promise.resolve(this.views));
    this._trigger();
  },
  positions(viewId, positions) {
    this.views = this.views.setIn([viewId, 'positions'], Object.assign({}, positions));
    ViewsActions.positions.promise(Promise.resolve(this.views));
    this._trigger();
  },
  _trigger() {
    this.trigger(this.views);
  },
});