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
import Reflux from 'reflux';

import { Store } from 'stores/StoreTypes';
import CombinedProvider from 'injection/CombinedProvider';
import { singletonActions, singletonStore } from 'views/logic/singleton';

const OriginalStreamsStore = CombinedProvider.get('Streams').StreamsStore;
const { SessionActions } = CombinedProvider.get('Session');

export const StreamsActions = singletonActions(
  'views.Streams',
  () => Reflux.createActions(['refresh']),
);

/* As the current implementation of the `StreamsStore` is not holding a state, using it requires to query the
   streams list for every component using it over and over again. This simple Reflux store is supposed to query the
   `StreamsStore` once and hold the result for future subscribers.
   */
export type Stream = {
  id: string;
  title: string;
};
export type StreamsStoreState = {
  streams: Array<Stream>;
};

export const StreamsStore: Store<StreamsStoreState> = singletonStore(
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
