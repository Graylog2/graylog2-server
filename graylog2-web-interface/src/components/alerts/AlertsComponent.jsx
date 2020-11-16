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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import Promise from 'bluebird';

import { Button } from 'components/graylog';
import { Alert } from 'components/alerts';
import { EntityList, PaginatedList, Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { StreamsStore } = CombinedProvider.get('Streams');

const ALERTS_REFRESH_INTERVAL = 10000;

const AlertsComponent = createReactClass({
  displayName: 'AlertsComponent',
  mixins: [Reflux.connect(AlertsStore), Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      displayAllAlerts: false,
      loading: false,
    };
  },

  componentDidMount() {
    this.loadData(this.currentPage, this.pageSize);
    this.interval = setInterval(() => this.fetchData(this.currentPage, this.pageSize), ALERTS_REFRESH_INTERVAL);
  },

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },

  currentPage: 1,
  pageSize: 10,

  loadData(pageNo, limit) {
    this.setState({ loading: true });
    const promises = this.fetchData(pageNo, limit);

    Promise.all(promises).finally(() => this.setState({ loading: false }));
  },

  fetchData(pageNo, limit) {
    const { displayAllAlerts } = this.state;

    return [
      AlertsActions.listAllPaginated((pageNo - 1) * limit, limit, displayAllAlerts ? 'any' : 'unresolved'),
      AlertConditionsActions.listAll(),
      AlertConditionsActions.available(),
      StreamsStore.listStreams().then((streams) => {
        this.setState({ streams: streams });
      }),
    ];
  },

  refreshData() {
    this.loadData(this.currentPage, this.pageSize);
  },

  _onToggleAllAlerts() {
    const { displayAllAlerts } = this.state;

    this.currentPage = 1;
    this.pageSize = 10;
    this.setState({ displayAllAlerts: !displayAllAlerts }, () => this.loadData(this.currentPage, this.pageSize));
  },

  _onChangePaginatedList(page, size) {
    this.currentPage = page;
    this.pageSize = size;
    this.loadData(page, size);
  },

  _formatAlert(alert) {
    const { allAlertConditions, streams, availableConditions } = this.state;

    const condition = allAlertConditions.find((alertCondition) => alertCondition.id === alert.condition_id);
    const stream = streams.find((s) => s.id === alert.stream_id);
    const conditionType = condition ? availableConditions[condition.type] : {};

    return (
      <Alert key={alert.id}
             alert={alert}
             alertCondition={condition}
             stream={stream}
             conditionType={conditionType} />
    );
  },

  _isLoading() {
    const { alerts, allAlertConditions, streams, availableConditions } = this.state;

    return !alerts || !allAlertConditions || !availableConditions || !streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { loading, displayAllAlerts, alerts } = this.state;

    return (
      <div>
        <div className="pull-right">
          <Button bsStyle="info" onClick={this.refreshData} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </Button>
          &nbsp;
          <Button bsStyle="info" onClick={this._onToggleAllAlerts}>
            Show {displayAllAlerts ? 'unresolved' : 'all'} alerts
          </Button>
        </div>
        <h2>{displayAllAlerts ? 'Alerts' : 'Unresolved alerts'}</h2>
        <p className="description">
          Check your alerts status from here. Currently displaying{' '}
          {displayAllAlerts ? 'all' : 'unresolved'} alerts.
        </p>

        <PaginatedList totalItems={alerts.total}
                       pageSize={this.pageSize}
                       onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle={displayAllAlerts ? 'info' : 'success'}
                      noItemsText={displayAllAlerts ? 'There are no alerts to display' : 'Good news! Currently there are no unresolved alerts.'}
                      items={alerts.alerts.map((alert) => this._formatAlert(alert))} />
        </PaginatedList>
      </div>
    );
  },
});

export default AlertsComponent;
