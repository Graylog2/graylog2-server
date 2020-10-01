// @flow strict
import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';
import { singletonActions, singletonStore } from 'views/logic/singleton';

const OriginalStreamsStore = CombinedProvider.get('Streams').StreamsStore;
const { SessionActions } = CombinedProvider.get('Session');

export const StreamsActions = singletonActions(
  'views.Streams',
  () => Reflux.createActions(['refresh']),
);

/* As the current typescript implementation of the `StreamsStore` is not holding a state, using it requires to query the
   streams list for every component using it over and over again. This simple Reflux store is supposed to query the
   `StreamsStore` once and hold the result for future subscribers.
   */
export const StreamsStore = singletonStore(
  'views.Streams',
  () => Reflux.createStore({
    listenables: [StreamsActions],
    streams: [],
    init() {
      this.refresh();

      SessionActions.logout.completed.listen(() => this.clear());
    },
    getInitialState() {
      return this._state();
    },
    refresh() {
      OriginalStreamsStore.listStreams().then((result) => {
        this.streams = result;
        this._trigger();
      });
    },
    clear() {
      this.streams = [];
      this._trigger();
    },
    _state() {
      return {
        streams: this.streams,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
