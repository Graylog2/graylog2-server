import React from 'react';
import Reflux from 'reflux';
import moment from 'moment';

import { Spinner, Timestamp } from 'components/common';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbackHistoryStore } = CombinedProvider.get('AlarmCallbackHistory');
const { AlarmCallbacksStore } = CombinedProvider.get('AlarmCallbacks');

const AlertTimeline = React.createClass({
  propTypes: {
    alert: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object.isRequired,
    condition: React.PropTypes.object,
    conditionType: React.PropTypes.object,
  },

  mixins: [Reflux.connect(AlarmCallbacksStore), Reflux.connect(AlarmCallbackHistoryStore)],

  _isLoading() {
    return !this.state.histories || !this.state.availableAlarmCallbacks;
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
      .sort((h1, h2) => {
        const h1Time = moment(h1.created_at);
        const h2Time = moment(h2.created_at);

        return (h1Time.isBefore(h2Time) ? -1 : h2Time.isBefore(h1Time) ? 1 : 0);
      })
      .forEach(history => {
        const configuration = history.alarmcallbackconfiguration;
        const type = this.state.availableAlarmCallbacks[configuration.type];
        let title;
        if (type) {
          title = <em>{type.name}</em>;
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
        <dd key="resolution-desc">This alert did not support resolution. It was marked as resolved when triggered.</dd>,
      );
    } else {
      if (this.props.alert.resolved_at) {
        formattedResolution.push(
          <dt key="resolution-title"><Timestamp dateTime={this.props.alert.resolved_at} /></dt>,
          <dd key="resolution-desc">Condition is no longer satisfied, alert is marked as resolved</dd>,
        );
      } else {
        formattedResolution.push(
          <dt key="resolution-title"><Timestamp dateTime={new Date()} /></dt>,
          <dd key="resolution-desc">Condition is still satisfied, alert is still <strong>unresolved</strong></dd>,
        );
      }
    }

    return formattedResolution;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const alert = this.props.alert;
    const condition = this.props.condition;
    const type = this.props.conditionType;
    const triggeredAtTimestamp = <Timestamp dateTime={alert.triggered_at} />;

    const title = (
      <span>
        <em>{condition.title || 'Unknown alert'}</em>{' '}
        ({type.name || condition.type || 'Unknown condition type'})
      </span>
    );

    return (
      <dl className="dl-horizontal">
        <dt>{triggeredAtTimestamp}</dt>
        <dd>Graylog checks {title} on stream <em>{this.props.stream.title}</em></dd>
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
