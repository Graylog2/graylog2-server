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
import URI from 'urijs';
import concat from 'lodash/concat';

import type { Store } from 'stores/StoreTypes';
import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

type EventsActionsType = {
  search: (params: {
    query?: string;
    page?: number;
    pageSize?: number;
    filter?: Record<string, unknown>;
    timerange?: Record<string, unknown>;
  }) => Promise<unknown>;
};

export const EventsActions = singletonActions('core.Events', () =>
  Reflux.createActions<EventsActionsType>({
    search: { asyncResult: true },
  }),
);

export const EventsStore: Store<{
  events: unknown;
  parameters: {
    page: unknown;
    pageSize: unknown;
    query: unknown;
    filter: unknown;
    timerange: unknown;
  };
  totalEvents: unknown;
  context: unknown;
}> = singletonStore('core.Events', () =>
  Reflux.createStore({
    listenables: [EventsActions],
    sourceUrl: '/events/search',
    events: undefined,
    totalEvents: undefined,
    context: undefined,
    parameters: {
      page: undefined,
      pageSize: undefined,
      query: undefined,
      filter: undefined,
      timerange: undefined,
    },

    getInitialState() {
      return this.getState();
    },

    propagateChanges() {
      this.trigger(this.getState());
    },

    getState() {
      return {
        events: this.events,
        parameters: this.parameters,
        totalEvents: this.totalEvents,
        context: this.context,
      };
    },

    eventsUrl({ segments = [], query = {} }: { segments?: Array<string>; query?: Record<string, unknown> }) {
      const uri = new URI(this.sourceUrl);
      const nextSegments = concat(uri.segment(), segments);

      uri.segmentCoded(nextSegments);
      uri.query(query);

      return URLUtils.qualifyUrl(uri.resource());
    },

    refresh() {
      const { query, page, pageSize, filter } = this.parameters;

      this.search({
        query: query,
        page: page,
        pageSize: pageSize,
        filter: filter,
      });
    },

    search({
      query = '',
      page = 1,
      pageSize = 25,
      filter = { alerts: 'only' },
      timerange = { type: 'relative', range: 3600 }, // 1 hour
    }: {
      query?: string;
      page?: number;
      pageSize?: number;
      filter?: Record<string, unknown>;
      timerange?: Record<string, unknown>;
    }) {
      const promise = fetch('POST', this.eventsUrl({}), {
        query: query,
        page: page,
        per_page: pageSize,
        filter: filter,
        timerange: timerange,
      });

      promise
        .then((response: unknown) => {
          const resp = response as Record<string, unknown>;
          const params = resp.parameters as Record<string, unknown>;
          this.events = resp.events;

          this.parameters = {
            query: params.query,
            page: params.page,
            pageSize: params.per_page,
            filter: params.filter,
            timerange: params.timerange,
          };

          this.totalEvents = resp.total_events;
          this.context = resp.context;
          this.propagateChanges();

          return response;
        })
        .catch((error: unknown) => {
          this.events = [];

          this.parameters = {
            query,
            page,
            pageSize,
            filter,
            timerange,
          };

          this.totalEvents = 0;

          this.context = {
            event_definitions: {},
            streams: {},
          };

          this.propagateChanges();

          return error;
        });

      EventsActions.search.promise(promise);
    },
  }),
);
