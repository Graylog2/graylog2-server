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
import lodash from 'lodash';

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { EventsActions } = CombinedProvider.get('Events');

const EventsStore = Reflux.createStore({
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

  eventsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);

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

  search({ query = '', page = 1, pageSize = 25, filter = { alerts: 'only' }, timerange }) {
    const promise = fetch('POST', this.eventsUrl({}), {
      query: query,
      page: page,
      per_page: pageSize,
      filter: filter,
      timerange: timerange,
    });

    promise.then((response) => {
      this.events = response.events;

      this.parameters = {
        query: response.parameters.query,
        page: response.parameters.page,
        pageSize: response.parameters.per_page,
        filter: response.parameters.filter,
        timerange: response.parameters.timerange,
      };

      this.totalEvents = response.total_events;
      this.context = response.context;
      this.propagateChanges();

      return response;
    });

    EventsActions.search.promise(promise);
  },
});

export default EventsStore;
