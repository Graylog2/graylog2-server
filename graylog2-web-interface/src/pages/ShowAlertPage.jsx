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

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Label, Tooltip, Button } from 'components/graylog';
import { DocumentTitle, OverlayElement, PageHeader, Spinner, Timestamp } from 'components/common';
import { AlertDetails } from 'components/alerts';
import DateTime from 'logic/datetimes/DateTime';
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import withParams from 'routing/withParams';

import style from './ShowAlertPage.css';

const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { StreamsStore } = CombinedProvider.get('Streams');

const ShowAlertPage = createReactClass({
  displayName: 'ShowAlertPage',

  propTypes: {
    params: PropTypes.object.isRequired,
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
    return !this.state.alert || !this.state.alertCondition || !this.state.availableConditions || !this.state.stream;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { alert } = this.state;
    const condition = this.state.alertCondition;
    const conditionExists = Object.keys(condition).length > 0;
    const conditionType = this.state.availableConditions[condition.type] || {};
    const { stream } = this.state;

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
              <ButtonToolbar>
                <LinkContainer to={Routes.LEGACY_ALERTS.LIST}>
                  <Button bsStyle="info">Alerts</Button>
                </LinkContainer>
                <OverlayElement overlay={conditionDetailsTooltip}
                                placement="top"
                                useOverlay={!condition.id}
                                trigger={['hover', 'focus']}>
                  <LinkContainer to={Routes.show_alert_condition(stream.id, condition.id)} disabled={!condition.id}>
                    <Button bsStyle="info">Condition details</Button>
                  </LinkContainer>
                </OverlayElement>
              </ButtonToolbar>
            </span>
          </PageHeader>

          <AlertDetails alert={alert} condition={conditionExists && condition} conditionType={conditionType} stream={stream} />
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(ShowAlertPage);
