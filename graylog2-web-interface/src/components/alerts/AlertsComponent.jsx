import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import Promise from 'bluebird';

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
    return [
      AlertsActions.listAllPaginated((pageNo - 1) * limit, limit, this.state.displayAllAlerts ? 'any' : 'unresolved'),
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
    this.currentPage = 1;
    this.pageSize = 10;
    this.setState({ displayAllAlerts: !this.state.displayAllAlerts }, () => this.loadData(this.currentPage, this.pageSize));
  },

  _onChangePaginatedList(page, size) {
    this.currentPage = page;
    this.pageSize = size;
    this.loadData(page, size);
  },

  _formatAlert(alert) {
    const condition = this.state.allAlertConditions.find(alertCondition => alertCondition.id === alert.condition_id);
    const stream = this.state.streams.find(s => s.id === alert.stream_id);
    const conditionType = condition ? this.state.availableConditions[condition.type] : {};

    return (
      <Alert key={alert.id}
             alert={alert}
             alertCondition={condition}
             stream={stream}
             conditionType={conditionType} />
    );
  },

  _isLoading() {
    return !this.state.alerts || !this.state.allAlertConditions || !this.state.availableConditions || !this.state.streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        <div className="pull-right">
          <Button bsStyle="info" onClick={this.refreshData} disabled={this.state.loading}>
            {this.state.loading ? 'Refreshing...' : 'Refresh'}
          </Button>
          &nbsp;
          <Button bsStyle="info" onClick={this._onToggleAllAlerts}>
            Show {this.state.displayAllAlerts ? 'unresolved' : 'all'} alerts
          </Button>
        </div>
        <h2>{this.state.displayAllAlerts ? 'Alerts' : 'Unresolved alerts'}</h2>
        <p className="description">
          Check your alerts status from here. Currently displaying{' '}
          {this.state.displayAllAlerts ? 'all' : 'unresolved'} alerts.
        </p>

        <PaginatedList totalItems={this.state.alerts.total}
                       pageSize={this.pageSize}
                       onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle={this.state.displayAllAlerts ? 'info' : 'success'}
                      noItemsText={this.state.displayAllAlerts ? 'There are no alerts to display' : 'Good news! Currently there are no unresolved alerts.'}
                      items={this.state.alerts.alerts.map(alert => this._formatAlert(alert))} />
        </PaginatedList>
      </div>
    );
  },
});

export default AlertsComponent;
