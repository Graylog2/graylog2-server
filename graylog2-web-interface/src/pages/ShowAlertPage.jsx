import React from 'react';
import Reflux from 'reflux';
import { Label } from 'react-bootstrap';

import { PageHeader, Spinner, Timestamp } from 'components/common';
import AlertDetails from 'components/alerts/AlertDetails';

import DateTime from 'logic/datetimes/DateTime';
import UserNotification from 'util/UserNotification';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

import style from './ShowAlertPage.css';

const ShowAlertPage = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(AlertsStore), Reflux.connect(AlertConditionsStore)],

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    AlertConditionsActions.available();
    AlertsActions.get(this.props.params.alertId).then(alert => {
      AlertConditionsActions.get(alert.stream_id, alert.condition_id, error => {
        if (error.additional && error.additional.status === 404) {
          this.setState({ alertCondition: {} });
        } else {
          UserNotification.error(`Fetching alert condition ${alert.condition_id} failed with status: ${error}`,
            'Could not get alert condition information');
        }
      });
    });
  },

  _isLoading() {
    return !this.state.alert || !this.state.alertCondition || !this.state.types;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    const alert = this.state.alert;
    const condition = this.state.alertCondition;
    const type = this.state.types[condition.type] || {};

    let statusLabel;
    let resolvedState;
    if (!alert.is_interval || alert.resolved_at) {
      statusLabel = <Label bsStyle="success">Resolved</Label>;
      const resolvedAtTime = alert.resolved_at || alert.triggered_at;
      if (resolvedAtTime) {
        resolvedState = (
          <span>
            This alert was resolved at <Timestamp dateTime={resolvedAtTime} format={DateTime.Formats.DATETIME} />.
          </span>
        );
      }
    } else {
      statusLabel = <Label bsStyle="danger">Unresolved</Label>;
      resolvedState = (
        <span>
          This alert was triggered at{' '}
          <Timestamp dateTime={alert.triggered_at} format={DateTime.Formats.DATETIME} />{' '}
          and is still unresolved.
        </span>
      );
    }

    const title = (
      <span>{condition.title || <em>Unknown alert</em>}&nbsp;
        <small>
          ({type.name || condition.type || 'Unknown condition type'})&nbsp;
          <span className={style.alertStatusLabel}>{statusLabel}</span>
        </small>
      </span>
    );

    return (
      <div>
        <PageHeader title={title}>
          <span>
            Check the timeline of this alert, including the notifications sent, and messages received during the alert.
          </span>

          <span>
            {resolvedState}
          </span>
        </PageHeader>

        <AlertDetails alert={alert} condition={condition} conditionType={type} />
      </div>
    );
  },
});

export default ShowAlertPage;
