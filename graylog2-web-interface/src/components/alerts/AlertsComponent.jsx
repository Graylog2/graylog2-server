import React from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertsStore, AlertsActions } = CombinedProvider.get('Alerts');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { StreamsStore } = CombinedProvider.get('Streams');

import { Alert } from 'components/alerts';
import { EntityList, PaginatedList, Spinner } from 'components/common';

const AlertsComponent = React.createClass({
  mixins: [Reflux.connect(AlertsStore), Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      displayAllAlerts: false,
    };
  },

  componentDidMount() {
    this.loadData(1, 10);
  },

  loadData(pageNo, limit) {
    AlertsActions.listAllPaginated((pageNo - 1) * limit, limit, this.state.displayAllAlerts ? 'all' : 'unresolved');
    AlertConditionsActions.listAll();
    AlertConditionsActions.available();
    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams: streams });
    });
  },

  _onToggleAllAlerts() {
    this.setState({ displayAllAlerts: !this.state.displayAllAlerts }, () => this.loadData(1, 10));
  },

  _onChangePaginatedList(page, size) {
    this.loadData(page, size);
  },

  _formatAlert(alert) {
    return (
      <Alert key={alert.id} alert={alert} alertConditions={this.state.allAlertConditions} streams={this.state.streams}
             conditionTypes={this.state.types} />
    );
  },

  _isLoading() {
    return !this.state.alerts || !this.state.allAlertConditions || !this.state.types || !this.state.streams;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        <div className="pull-right">
          <Button bsStyle="info" onClick={this._onToggleAllAlerts}>
            Show {this.state.displayAllAlerts ? 'unresolved' : 'all'} alerts
          </Button>
        </div>
        <h2>{this.state.displayAllAlerts ? 'Alerts' : 'Unresolved alerts'}</h2>
        <p className="description">
          Check your alerts status from here. Currently displaying{' '}
          {this.state.displayAllAlerts ? 'all' : 'unresolved'} alerts.
        </p>

        <PaginatedList totalItems={this.state.alerts.total} onChange={this._onChangePaginatedList}
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
