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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner, Timestamp } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import { sortByDate } from 'util/SortUtils';

import style from './AlertTimeline.css';

const { AlarmCallbackHistoryStore } = CombinedProvider.get('AlarmCallbackHistory');
const { AlertNotificationsStore } = CombinedProvider.get('AlertNotifications');

const AlertTimeline = createReactClass({
  displayName: 'AlertTimeline',

  propTypes: {
    alert: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
    condition: PropTypes.object,
    conditionType: PropTypes.object,
  },

  mixins: [Reflux.connect(AlertNotificationsStore), Reflux.connect(AlarmCallbackHistoryStore)],

  _isLoading() {
    return !this.state.histories || !this.state.availableNotifications;
  },

  _historiesTimeline(lastEventTime) {
    const formattedHistories = [];

    if (this.state.histories.length === 0) {
      return [
        <dt key="history-title"><Timestamp dateTime={lastEventTime} /></dt>,
        <dd key="history-desc">No notifications were configured for this alert</dd>,
      ];
    }

    this.state.histories
      .sort((h1, h2) => sortByDate(h1.created_at, h2.created_at))
      .forEach((history) => {
        const configuration = history.alarmcallbackconfiguration;
        const type = this.state.availableNotifications[configuration.type];
        let title;

        if (type) {
          title = <span><em>{configuration.title || 'Untitled notification'}</em> ({type.name})</span>;
        } else {
          title = <span><em>Unknown notification</em> <small>({configuration.type})</small></span>;
        }

        formattedHistories.push(
          <dt key={`${history.id}-title`}><Timestamp dateTime={history.created_at} /></dt>,
          (<dd key={`${history.id}-desc`}>
            Graylog {history.result.type === 'error' ? 'could not send' : 'sent'} {title} notification
          </dd>),
        );
      });

    return formattedHistories;
  },

  _resolutionTimeline() {
    const formattedResolution = [];

    if (!this.props.alert.is_interval) {
      // Old alert without a resolution_at field
      formattedResolution.push(
        <dt key="resolution-title"><Timestamp dateTime={this.props.alert.triggered_at} /></dt>,
        <dd key="resolution-desc">This alert does not support resolution. It was marked as resolved when triggered.</dd>,
      );
    } else if (this.props.alert.resolved_at) {
      formattedResolution.push(
        <dt key="resolution-title"><Timestamp dateTime={this.props.alert.resolved_at} /></dt>,
        <dd key="resolution-desc">Condition is no longer satisfied, alert is marked as resolved</dd>,
      );
    } else {
      const conditionParameters = this.props.alert.condition_parameters || {};
      const repeatNotifications = conditionParameters.repeat_notifications || false;
      const notificationsText = (repeatNotifications
        ? 'Condition is configured to repeat notifications, Graylog will send notifications when evaluating the condition until it is no longer satisfied'
        : 'Condition is configured to not repeat notifications');

      formattedResolution.push(
        <dt key="notifications-title"><Timestamp dateTime={new Date()} /></dt>,
        <dd key="notifications-desc">{notificationsText}</dd>,
        <dt key="resolution-title"><Timestamp dateTime={new Date()} /></dt>,
        <dd key="resolution-desc">Condition is still satisfied, <strong>alert is unresolved</strong></dd>,
      );
    }

    return formattedResolution;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { alert } = this.props;
    const conditionExists = this.props.condition && Object.keys(this.props.condition).length > 0;
    const condition = this.props.condition || {};
    const type = this.props.conditionType;
    const triggeredAtTimestamp = <Timestamp dateTime={alert.triggered_at} />;

    const title = (
      <span>
        <em>{conditionExists ? condition.title || 'Untitled condition' : 'Unknown condition'}</em>{' '}
        ({type.name || condition.type || 'Unknown condition type'})
      </span>
    );

    return (
      <dl className={`dl-horizontal ${style.alertTimeline}`}>
        <dt>{triggeredAtTimestamp}</dt>
        <dd>Graylog checks {title} condition on stream <em>{this.props.stream.title}</em></dd>
        <dt>{triggeredAtTimestamp}</dt>
        <dd>{alert.description}</dd>
        <dt>{triggeredAtTimestamp}</dt>
        <dd>Graylog triggers an alert for {title} and starts sending notifications</dd>
        {this._historiesTimeline(alert.triggered_at)}
        {this._resolutionTimeline()}
      </dl>
    );
  },
});

export default AlertTimeline;
