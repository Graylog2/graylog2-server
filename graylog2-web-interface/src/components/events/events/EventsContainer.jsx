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
import React from 'react';
import PropTypes from 'prop-types';
import isObject from 'lodash/isObject';
import debounce from 'lodash/debounce';

import { Spinner } from 'components/common';
import UserNotification from 'util/UserNotification';
import connect from 'stores/connect';
import Store from 'logic/local-storage/Store';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { EventDefinitionsActions, EventDefinitionsStore } from 'stores/event-definitions/EventDefinitionsStore';
import { EventsActions, EventsStore } from 'stores/events/EventsStore';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';

import Events, { PAGE_SIZES, EVENTS_MAX_OFFSET_LIMIT } from './Events';

const LOCAL_STORAGE_ITEM = 'events-last-search';
const SEARCH_DEBOUNCE_THRESHOLD = 500;

const fetchEvents = ({ page, pageSize, query, filter, timerange }) => {
  Store.set(LOCAL_STORAGE_ITEM, { filter: filter, timerange: timerange });

  return EventsActions.search({
    query: query,
    page: page,
    pageSize: pageSize,
    filter: filter,
    timerange: timerange,
  }).catch((error) => {
    UserNotification.error(`Fetching alerts failed with status: ${error}`);
  });
};

const fetchEventDefinitions = () => EventDefinitionsActions.listPaginated({});

class EventsContainer extends React.Component {
  static propTypes = {
    events: PropTypes.object.isRequired,
    eventDefinitions: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    streamId: PropTypes.string,
    paginationQueryParameter: PropTypes.object.isRequired,
  };

  static defaultProps = {
    streamId: '',
  };

  componentDidMount() {
    const { streamId } = this.props;
    const { page, pageSize } = this.props.paginationQueryParameter;
    const lastSearch = Store.get(LOCAL_STORAGE_ITEM) || {};

    const params = { page, pageSize };

    if (streamId) {
      params.query = `source_streams:${streamId}`;
    }

    if (lastSearch && isObject(lastSearch)) {
      params.filter = lastSearch.filter;
      params.timerange = lastSearch.timerange;
    }

    fetchEvents(params);
    fetchEventDefinitions();
  }

  handlePageChange = (nextPage, nextPageSize) => {
    if (nextPage * nextPageSize <= EVENTS_MAX_OFFSET_LIMIT) {
      const { events } = this.props;

      fetchEvents({
        page: nextPage,
        pageSize: nextPageSize,
        query: events.parameters.query,
        filter: events.parameters.filter,
        timerange: events.parameters.timerange,
      });
    }
  };

  handleQueryChange = debounce((nextQuery, callback = () => {}) => {
    const { events } = this.props;
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    const promise = fetchEvents({
      query: nextQuery,
      pageSize,
      filter: events.parameters.filter,
      timerange: events.parameters.timerange,
    });

    promise.finally(callback);
  }, SEARCH_DEBOUNCE_THRESHOLD);

  handleAlertFilterChange = (nextAlertFilter) => () => {
    const { events } = this.props;
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    fetchEvents({
      query: events.parameters.query,
      pageSize: pageSize,
      filter: { alerts: nextAlertFilter },
      timerange: events.parameters.timerange,
    });
  };

  handleTimeRangeChange = (timeRangeType, range) => {
    const { events } = this.props;
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    fetchEvents({
      query: events.parameters.query,
      pageSize,
      filter: events.parameters.filter,
      timerange: { type: timeRangeType, range: range },
    });
  };

  handleSearchReload = (callback = () => {}) => {
    const { events } = this.props;
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    const promise = fetchEvents({
      query: events.parameters.query,
      pageSize,
      filter: events.parameters.filter,
      timerange: events.parameters.timerange,
    });

    promise.finally(callback);
  };

  render() {
    const { events, eventDefinitions, currentUser } = this.props;
    const isLoading = !events.events || !eventDefinitions.eventDefinitions;

    if (isLoading) {
      return <Spinner text="Loading Events information..." />;
    }

    return (
      <Events events={events.events}
              parameters={events.parameters}
              totalEvents={events.totalEvents}
              currentUser={currentUser}
              totalEventDefinitions={eventDefinitions.pagination.grandTotal}
              context={events.context}
              onQueryChange={this.handleQueryChange}
              onPageChange={this.handlePageChange}
              onAlertFilterChange={this.handleAlertFilterChange}
              onTimeRangeChange={this.handleTimeRangeChange}
              onSearchReload={this.handleSearchReload} />
    );
  }
}

export default connect(withPaginationQueryParameter(EventsContainer, { pageSizes: PAGE_SIZES }), {
  events: EventsStore,
  eventDefinitions: EventDefinitionsStore,
  currentUser: CurrentUserStore,
}, ({ currentUser, ...otherProps }) => ({
  ...otherProps,
  currentUser: currentUser.currentUser,
}));
