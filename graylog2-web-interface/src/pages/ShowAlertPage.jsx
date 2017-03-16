import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, Label, Tooltip } from 'react-bootstrap';

import { DocumentTitle, OverlayElement, PageHeader, Spinner, Timestamp } from 'components/common';
import { AlertDetails } from 'components/alerts';

import DateTime from 'logic/datetimes/DateTime';
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { StreamsStore } = CombinedProvider.get('Streams');

import style from './ShowAlertPage.css';

const ShowAlertPage = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(AlertsStore), Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      stream: undefined,
    };
  },

  componentDidMount() {
    this._loadData();
  },

  componentDidUpdate(prevProps, prevState) {
    if (prevState.alert !== this.state.alert) {
      this._loadAlertDetails(this.state.alert);
    }
  },

  _loadData() {
    AlertConditionsActions.available();
    AlertsActions.get(this.props.params.alertId);
  },

  _loadAlertDetails(alert) {
    StreamsStore.get(alert.stream_id, (stream) => {
      this.setState({ stream: stream });
    });
    AlertConditionsActions.get(alert.stream_id, alert.condition_id, (error) => {
      if (error.additional && error.additional.status === 404) {
        this.setState({ alertCondition: {} });
      } else {
        UserNotification.error(`Fetching alert condition ${alert.condition_id} failed with status: ${error}`,
          'Could not get alert condition information');
      }
    });
  },

  _isLoading() {
    return !this.state.alert || !this.state.alertCondition || !this.state.types || !this.state.stream;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const alert = this.state.alert;
    const condition = this.state.alertCondition;
    const conditionExists = Object.keys(condition).length > 0;
    const type = this.state.types[condition.type] || {};
    const stream = this.state.stream;

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
      <span>{conditionExists ? condition.title || 'Untitled alert' : 'Unknown alert'}&nbsp;
        <small>
          on stream <em>{stream.title}</em>&nbsp;
          <span className={style.alertStatusLabel}>{statusLabel}</span>
        </small>
      </span>
    );

    const conditionDetailsTooltip = (
      <Tooltip id="disabled-condition-details">
        The condition was most likely deleted since the alert was triggered, no details available.
      </Tooltip>
    );

    return (
      <DocumentTitle title={`${conditionExists ? condition.title || 'Untitled alert' : 'Unknown alert'} on stream ${stream.title}`}>
        <div>
          <PageHeader title={title}>
            <span>
              Check the timeline of this alert, including the notifications sent, and messages received during the
              alert.
            </span>

            <span>
              {resolvedState}
            </span>

            <span>
              <OverlayElement overlay={conditionDetailsTooltip} placement="top" useOverlay={!condition.id}
                              trigger={['hover', 'focus']}>
                <LinkContainer to={Routes.show_alert_condition(stream.id, condition.id)} disabled={!condition.id}>
                  <Button bsStyle="info">Condition details</Button>
                </LinkContainer>
              </OverlayElement>
              &nbsp;
              <LinkContainer to={Routes.ALERTS.LIST}>
                <Button bsStyle="info">Alerts overview</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <AlertDetails alert={alert} condition={conditionExists && condition} conditionType={type} stream={stream} />
        </div>
      </DocumentTitle>
    );
  },
});

export default ShowAlertPage;
