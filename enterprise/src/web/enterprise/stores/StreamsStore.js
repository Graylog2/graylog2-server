import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';

const OriginalStreamsStore = CombinedProvider.get('Streams').StreamsStore;

/* As the current typescript implementation of the `StreamsStore` is not holding a state, using it requires to query the
   streams list for every component using it over and over again. This simple Reflux store is supposed to query the
   `StreamsStore` once and hold the result for future subscribers.
   */
export const StreamsStore = Reflux.createStore({
  streams: [],
  init() {
    OriginalStreamsStore.listStreams().then((result) => {
      this.streams = result;
      this._trigger();
    });
  },
  getInitialState() {
    return this._state();
  },
  _state() {
    return {
      streams: this.streams,
    };
  },
  _trigger() {
    this.trigger(this._state());
  },
});
