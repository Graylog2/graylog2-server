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

import { Spinner } from 'components/common';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import EventNotifications from './EventNotifications';

// Import built-in Event Notification Types
import {} from '../event-notification-types';

const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class EventNotificationsContainer extends React.Component {
  static propTypes = {
    notifications: PropTypes.object,
  };

  static defaultProps = {
    notifications: undefined,
  };

  state = {
    testResult: {},
  };

  componentDidMount() {
    this.fetchData({});
  }

  componentWillUnmount() {
    if (this.testPromise) {
      this.testPromise.cancel();
    }
  }

  fetchData = ({ page, pageSize, query }) => {
    return EventNotificationsActions.listPaginated({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    const { notifications } = this.props;

    this.fetchData({ page: nextPage, pageSize: nextPageSize, query: notifications.query });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { notifications } = this.props;
    const promise = this.fetchData({ query: nextQuery, pageSize: notifications.pagination.pageSize });

    promise.finally(callback);
  };

  handleDelete = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete "${definition.title}"?`)) {
        EventNotificationsActions.delete(definition);
      }
    };
  };

  handleTest = (definition) => {
    return () => {
      this.setState({ testResult: { isLoading: true, id: definition.id } });
      let testResult = { isLoading: false };

      if (this.testPromise) {
        this.testPromise.cancel();
      }

      this.testPromise = EventNotificationsActions.testPersisted(definition);

      this.testPromise
        .then(
          (response) => {
            testResult = {
              isLoading: false,
              id: definition.id,
              error: false,
              message: 'Notification was executed successfully.',
            };

            return response;
          },
          (errorResponse) => {
            testResult = { isLoading: false, id: definition.id, error: true };

            if (errorResponse.status !== 400 || !errorResponse.additional.body || !errorResponse.additional.body.failed) {
              testResult.message = errorResponse.responseMessage || 'Unknown errorResponse, please check your Graylog server logs.';
            }

            return errorResponse;
          },
        )
        .finally(() => {
          this.setState({ testResult: testResult });
          this.testPromise = undefined;
        });
    };
  };

  render() {
    const { notifications } = this.props;
    const { testResult } = this.state;

    if (!notifications.notifications) {
      return <Spinner text="Loading Notifications information..." />;
    }

    return (
      <EventNotifications notifications={notifications.notifications}
                          pagination={notifications.pagination}
                          query={notifications.query}
                          testResult={testResult}
                          onPageChange={this.handlePageChange}
                          onQueryChange={this.handleQueryChange}
                          onDelete={this.handleDelete}
                          onTest={this.handleTest} />
    );
  }
}

export default connect(EventNotificationsContainer, { notifications: EventNotificationsStore });
