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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { Alert, Button } from 'components/graylog';
import { PaginatedList, Spinner, Timestamp } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';
import UserNotification from 'util/UserNotification';

const { UniversalSearchStore } = CombinedProvider.get('UniversalSearch');

class AlertMessages extends React.Component {
  static propTypes = {
    alert: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
  };

  state = {
    messages: undefined,
    totalMessages: 0,
  };

  componentDidMount() {
    this._loadData();
  }

  PAGE_SIZE = 20;

  _getFrom = () => {
    const momentFrom = DateTime.parseFromString(this.props.alert.triggered_at).toMoment();

    return momentFrom.subtract(1, 'minute').toISOString();
  };

  _getTo = () => {
    const { alert } = this.props;
    let momentTo;

    if (alert.is_interval) {
      momentTo = (alert.resolved_at ? DateTime.parseFromString(alert.resolved_at).toMoment().add(1, 'minute') : DateTime.now());
    } else {
      momentTo = DateTime.parseFromString(alert.triggered_at).toMoment().add(1, 'minute');
    }

    return momentTo.toISOString();
  };

  _loadData = (page) => {
    const searchParams = {
      from: this._getFrom(),
      to: this._getTo(),
    };
    const promise = UniversalSearchStore.search('absolute', '*', searchParams, this.props.stream.id, this.PAGE_SIZE,
      page || 1, 'timestamp', 'asc', undefined, false);

    promise.then(
      (response) => {
        if (response.total_results > 0) {
          this.setState({ messages: response.messages, totalMessages: response.total_results });
        } else {
          this.setState({ messages: [], totalMessages: 0 });
        }
      },
      (error) => {
        UserNotification.error(`Fetching messages during alert failed with error: ${error}`,
          'Could not get messages during alert');
      },
    );
  };

  _isLoading = () => {
    return !this.state.messages;
  };

  _onPageChange = (page) => {
    this._loadData(page);
  };

  _formatMessages = (messages) => {
    return messages
      .map((message) => {
        return (
          <tr key={`${message.index}-${message.id}`}>
            <td><Timestamp dateTime={message.formatted_fields.timestamp} /></td>
            <td>{message.formatted_fields.message}</td>
          </tr>
        );
      });
  };

  _formatAlertTimeRange = () => {
    return (
      <span>
        (
        <Timestamp dateTime={this._getFrom()} format={DateTime.Formats.DATETIME} />&nbsp;&#8211;&nbsp;
        <Timestamp dateTime={this._getTo()} format={DateTime.Formats.DATETIME} />
        )
      </span>
    );
  };

  render() {
    const timeRange = {
      rangetype: 'absolute',
      from: this._getFrom(),
      to: this._getTo(),
    };

    const title = (
      <div>
        <div className="pull-right">
          <LinkContainer to={Routes.stream_search(this.props.stream.id, '*', timeRange)}>
            <Button bsStyle="info">Open in search page</Button>
          </LinkContainer>
        </div>
        <h2>Messages evaluated</h2>
        <p>
          These are the messages evaluated around the time of the alert {this._formatAlertTimeRange()} in stream{' '}
          <em>{this.props.stream.title}</em>.
        </p>
      </div>
    );

    if (this._isLoading()) {
      return (
        <div>
          {title}
          <Spinner />
        </div>
      );
    }

    const { messages } = this.state;

    if (messages.length === 0) {
      return (
        <div>
          {title}
          <Alert bsStyle="info">No search results found during the time of the alert.</Alert>
        </div>
      );
    }

    return (
      <div>
        {title}
        <PaginatedList pageSize={this.PAGE_SIZE}
                       onChange={this._onPageChange}
                       totalItems={this.state.totalMessages}
                       showPageSizeSelect={false}>
          <div className="table-responsive">
            <table className="table table-striped table-hover table-condensed">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {this._formatMessages(messages)}
              </tbody>
            </table>
          </div>
        </PaginatedList>
      </div>
    );
  }
}

export default AlertMessages;
