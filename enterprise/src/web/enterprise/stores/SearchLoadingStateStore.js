import Reflux from 'reflux';

export const SearchLoadingStateActions = Reflux.createActions(['loading', 'finished']);
export const SearchLoadingStateStore = Reflux.createStore({
  listenables: [SearchLoadingStateActions],
  isLoading: false,
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
});
