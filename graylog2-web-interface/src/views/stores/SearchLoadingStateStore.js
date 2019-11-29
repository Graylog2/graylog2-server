// @flow strict
import Reflux from 'reflux';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import { SearchActions } from './SearchStore';

export const SearchLoadingStateActions = singletonActions(
  'views.SearchLoadingState',
  () => Reflux.createActions(['loading', 'finished']),
);

export const SearchLoadingStateStore = singletonStore(
  'views.SearchLoadingState',
  () => Reflux.createStore({
    listenables: [SearchLoadingStateActions],
    isLoading: false,
    init() {
      SearchActions.execute.listen(this.loading);
      SearchActions.execute.completed.listen(this.finished);
      SearchActions.reexecuteSearchTypes.listen(this.loading);
      SearchActions.reexecuteSearchTypes.completed.listen(this.finished);
    },
    getInitialState() {
      return this._state();
    },
    loading() {
      this.isLoading = true;
      this._trigger();
    },
    finished() {
      this.isLoading = false;
      this._trigger();
    },
    _state() {
      return {
        isLoading: this.isLoading,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
