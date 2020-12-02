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
import PropTypes from 'prop-types';
import Reflux from 'reflux';

import { Alert } from 'components/alerts';
import { EntityList, PaginatedList, Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');

const ALERTS_REFRESH_INTERVAL = 10000;

const StreamAlerts = createReactClass({
  displayName: 'StreamAlerts',
  propTypes: {
    stream: PropTypes.object.isRequired,
    alertConditions: PropTypes.array.isRequired,
    availableConditions: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(AlertsStore)],

  getInitialState() {
    return {
      loading: true,
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
  pageSize: 3,

  loadData(pageNo, limit) {
    this.setState({ loading: true });
    this.fetchData(pageNo, limit).finally(() => this.setState({ loading: false }));
  },

  // Loads data without setting the loading state, which makes the page jump
  fetchData(pageNo, limit) {
    return AlertsActions.listPaginated(this.props.stream.id, (pageNo - 1) * limit, limit, 'unresolved');
  },

  _onChangePaginatedList(page, size) {
    this.currentPage = page;
    this.pageSize = size;
    this.loadData(page, size);
  },

  _formatAlert(alert) {
    const condition = this.props.alertConditions.find((alertCondition) => alertCondition.id === alert.condition_id);
    const conditionType = condition ? this.props.availableConditions[condition.type] : {};

    return (
      <Alert key={alert.id}
             alert={alert}
             alertCondition={condition}
             stream={this.props.stream}
             conditionType={conditionType} />
    );
  },

  render() {
    if (this.state.loading) {
      return <Spinner />;
    }

    return (
      <div>
        <h2>Unresolved Alerts</h2>
        <p className="description">
          These are the Alerts for this Stream that require your attention. Alerts will be resolved automatically
          when the Condition that triggered them is no longer satisfied.
        </p>

        <PaginatedList totalItems={this.state.alerts.total}
                       pageSize={this.pageSize}
                       onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle="success"
                      noItemsText="Good news! Currently there are no unresolved alerts on this stream."
                      items={this.state.alerts.alerts.map((alert) => this._formatAlert(alert))} />
        </PaginatedList>
      </div>
    );
  },
});

export default StreamAlerts;
